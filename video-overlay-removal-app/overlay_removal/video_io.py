"""Video reading, encoding and metadata handling.

Uses OpenCV for frame decoding and ffmpeg (as a subprocess) for encoding,
so that we get full control over the output codec, color metadata and
audio muxing. This keeps the Python dependency list small (no
ffmpeg-python / moviepy needed) while still producing standard,
well-formed MP4/H.264/H.265 output.
"""

from __future__ import annotations

import json
import shutil
import subprocess
from dataclasses import dataclass, field
from typing import Iterator, Optional

import cv2
import numpy as np


class FFmpegNotFoundError(RuntimeError):
    """Raised when the ffmpeg/ffprobe binaries are not available on PATH."""


def _require_ffmpeg() -> None:
    if shutil.which("ffmpeg") is None or shutil.which("ffprobe") is None:
        raise FFmpegNotFoundError(
            "ffmpeg and ffprobe must be installed and on PATH. "
            "Install with your OS package manager, e.g. `apt install ffmpeg` "
            "or `brew install ffmpeg`."
        )


@dataclass
class VideoMeta:
    """Container + stream metadata probed from the source video."""

    width: int
    height: int
    fps: float
    frame_count: int
    duration: float
    has_audio: bool
    color_range: Optional[str] = None
    color_space: Optional[str] = None
    color_transfer: Optional[str] = None
    color_primaries: Optional[str] = None
    rotation: int = 0
    raw_stream: dict = field(default_factory=dict, repr=False)


def probe_video(path: str) -> VideoMeta:
    """Read container/stream metadata with ffprobe (resolution, fps, color, audio)."""
    _require_ffmpeg()
    cmd = [
        "ffprobe",
        "-v",
        "error",
        "-print_format",
        "json",
        "-show_format",
        "-show_streams",
        path,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, check=True)
    data = json.loads(result.stdout)

    video_stream = next(s for s in data["streams"] if s["codec_type"] == "video")
    audio_streams = [s for s in data["streams"] if s["codec_type"] == "audio"]

    num, den = (video_stream.get("r_frame_rate", "25/1")).split("/")
    fps = float(num) / float(den) if float(den) != 0 else float(num)

    duration = float(data["format"].get("duration", video_stream.get("duration", 0.0)) or 0.0)
    frame_count = int(video_stream.get("nb_frames", 0) or 0)
    if frame_count == 0 and duration and fps:
        frame_count = int(round(duration * fps))

    rotation = 0
    for side_data in video_stream.get("side_data_list", []) or []:
        if "rotation" in side_data:
            rotation = int(side_data["rotation"])

    return VideoMeta(
        width=int(video_stream["width"]),
        height=int(video_stream["height"]),
        fps=fps,
        frame_count=frame_count,
        duration=duration,
        has_audio=len(audio_streams) > 0,
        color_range=video_stream.get("color_range"),
        color_space=video_stream.get("color_space"),
        color_transfer=video_stream.get("color_transfer"),
        color_primaries=video_stream.get("color_primaries"),
        rotation=rotation,
        raw_stream=video_stream,
    )


def read_frames(path: str) -> Iterator[np.ndarray]:
    """Yield BGR frames from a video file, in order, using OpenCV."""
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        raise IOError(f"Could not open video file: {path}")
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            yield frame
    finally:
        cap.release()


def sample_frames(path: str, max_samples: int = 24) -> list[np.ndarray]:
    """Grab up to `max_samples` frames evenly spaced across the video.

    Used by the static-overlay detector, which only needs a representative
    subset of frames rather than the whole video.
    """
    meta = probe_video(path)
    total = meta.frame_count or 1
    step = max(1, total // max_samples)
    frames = []
    for i, frame in enumerate(read_frames(path)):
        if i % step == 0:
            frames.append(frame)
        if len(frames) >= max_samples:
            break
    return frames


_CODEC_ENCODERS = {
    "h264": "libx264",
    "h265": "libx265",
    "hevc": "libx265",
}


def open_frame_encoder(
    out_path: str,
    width: int,
    height: int,
    fps: float,
    codec: str = "h264",
    crf: int = 20,
    meta: Optional[VideoMeta] = None,
) -> subprocess.Popen:
    """Start an ffmpeg process that reads raw BGR24 frames from stdin.

    Returns the Popen handle; callers write raw frame bytes to
    `process.stdin` and call `process.stdin.close()` + `process.wait()`
    when done. Preserves original resolution and frame rate exactly, and
    forwards known color-space metadata so the output doesn't silently
    shift to a default color interpretation.
    """
    _require_ffmpeg()
    encoder = _CODEC_ENCODERS.get(codec.lower())
    if encoder is None:
        raise ValueError(f"Unsupported codec '{codec}'. Use one of {sorted(set(_CODEC_ENCODERS))}.")

    cmd = [
        "ffmpeg",
        "-y",
        "-hide_banner",
        "-loglevel",
        "error",
        "-f",
        "rawvideo",
        "-pix_fmt",
        "bgr24",
        "-s",
        f"{width}x{height}",
        "-r",
        f"{fps:.6f}",
        "-i",
        "-",
        "-an",
        "-c:v",
        encoder,
        "-crf",
        str(crf),
        "-pix_fmt",
        "yuv420p",
    ]

    if meta is not None:
        if meta.color_range:
            cmd += ["-color_range", meta.color_range]
        if meta.color_space:
            cmd += ["-colorspace", meta.color_space]
        if meta.color_transfer:
            cmd += ["-color_trc", meta.color_transfer]
        if meta.color_primaries:
            cmd += ["-color_primaries", meta.color_primaries]

    cmd.append(out_path)

    return subprocess.Popen(cmd, stdin=subprocess.PIPE)


def mux_audio_and_finalize(video_only_path: str, source_path: str, final_out_path: str, has_audio: bool) -> None:
    """Combine the re-encoded (overlay-free) video with the source audio track.

    Audio is stream-copied (no re-encode) so quality is untouched. Source
    metadata (creation time, rotation tags, etc.) is copied through where
    the container supports it.
    """
    _require_ffmpeg()
    if has_audio:
        cmd = [
            "ffmpeg",
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            video_only_path,
            "-i",
            source_path,
            "-map",
            "0:v:0",
            "-map",
            "1:a:0",
            "-map_metadata",
            "1",
            "-c:v",
            "copy",
            "-c:a",
            "copy",
            "-shortest",
            final_out_path,
        ]
    else:
        cmd = [
            "ffmpeg",
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            video_only_path,
            "-map",
            "0:v:0",
            "-map_metadata",
            "0",
            "-c:v",
            "copy",
            final_out_path,
        ]
    subprocess.run(cmd, check=True)
