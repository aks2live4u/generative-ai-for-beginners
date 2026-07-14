import os
import shutil
import subprocess
import tempfile

import numpy as np
import pytest

from overlay_removal.inpaint import OpenCVInpaintBackend
from overlay_removal.pipeline import OverlayRemovalPipeline
from overlay_removal.video_io import open_frame_encoder, probe_video

FFMPEG_AVAILABLE = shutil.which("ffmpeg") is not None and shutil.which("ffprobe") is not None

WIDTH, HEIGHT, FPS, N_FRAMES = 96, 64, 10.0, 20
OVERLAY_BOX = (60, 4, 30, 14)  # x, y, w, h - "timestamp"-like patch


def _overlay_plate(ow: int, oh: int) -> np.ndarray:
    """A dark plate with bright digit-segment bars, standing in for a
    burned-in timestamp/caption: textured enough for the auto-detector's
    edge check, unlike a single flat color block."""
    plate = np.full((oh, ow, 3), 20, dtype=np.uint8)
    plate[3:6, 3 : ow - 3] = (230, 230, 230)
    plate[8:11, 3 : ow - 3] = (230, 230, 230)
    return plate


def _build_source_video(path: str, with_audio: bool) -> None:
    rng = np.random.default_rng(42)
    ox, oy, ow, oh = OVERLAY_BOX
    plate = _overlay_plate(ow, oh)

    video_path = path
    if with_audio:
        fd, video_path = tempfile.mkstemp(suffix=".mp4")
        os.close(fd)

    proc = open_frame_encoder(video_path, WIDTH, HEIGHT, FPS, codec="h264", crf=18)
    for _ in range(N_FRAMES):
        frame = rng.integers(0, 255, size=(HEIGHT, WIDTH, 3), dtype=np.uint8)
        frame[oy : oy + oh, ox : ox + ow] = plate
        proc.stdin.write(frame.astype(np.uint8).tobytes())
    proc.stdin.close()
    assert proc.wait() == 0

    if with_audio:
        cmd = [
            "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
            "-i", video_path,
            "-f", "lavfi", "-i", "anullsrc=r=44100:cl=stereo",
            "-shortest",
            "-c:v", "copy", "-c:a", "aac",
            path,
        ]
        subprocess.run(cmd, check=True)
        os.remove(video_path)


@pytest.mark.skipif(not FFMPEG_AVAILABLE, reason="ffmpeg/ffprobe not installed")
def test_pipeline_removes_overlay_and_preserves_format(tmp_path):
    src = str(tmp_path / "source.mp4")
    out = str(tmp_path / "output.mp4")
    _build_source_video(src, with_audio=False)

    src_meta = probe_video(src)
    assert src_meta.width == WIDTH
    assert src_meta.height == HEIGHT
    assert src_meta.has_audio is False

    pipeline = OverlayRemovalPipeline(backend=OpenCVInpaintBackend(temporal_blend=0.3), dilate=2, feather=3)
    result = pipeline.run(src, out, boxes=[OVERLAY_BOX], codec="h264", crf=20)

    assert os.path.exists(out)
    assert result.frame_count == N_FRAMES

    out_meta = probe_video(out)
    assert out_meta.width == WIDTH
    assert out_meta.height == HEIGHT
    assert abs(out_meta.fps - FPS) < 0.5
    assert out_meta.has_audio is False


@pytest.mark.skipif(not FFMPEG_AVAILABLE, reason="ffmpeg/ffprobe not installed")
def test_pipeline_preserves_audio_track(tmp_path):
    src = str(tmp_path / "source_audio.mp4")
    out = str(tmp_path / "output_audio.mp4")
    _build_source_video(src, with_audio=True)

    src_meta = probe_video(src)
    assert src_meta.has_audio is True

    pipeline = OverlayRemovalPipeline(backend=OpenCVInpaintBackend(temporal_blend=0.0))
    pipeline.run(src, out, boxes=[OVERLAY_BOX], codec="h264", crf=23)

    out_meta = probe_video(out)
    assert out_meta.has_audio is True


@pytest.mark.skipif(not FFMPEG_AVAILABLE, reason="ffmpeg/ffprobe not installed")
def test_pipeline_auto_detect(tmp_path):
    src = str(tmp_path / "source.mp4")
    out = str(tmp_path / "output.mp4")
    _build_source_video(src, with_audio=False)

    pipeline = OverlayRemovalPipeline(backend=OpenCVInpaintBackend())
    result = pipeline.run(
        src, out, auto_detect=True, sample_count=N_FRAMES,
        detector_kwargs={"variance_threshold": 10.0},
    )

    assert os.path.exists(out)
    assert len(result.boxes) > 0
