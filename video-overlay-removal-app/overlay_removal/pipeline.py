"""End-to-end overlay removal pipeline.

Import -> detect/define overlay region -> build mask -> inpaint (with
temporal consistency) -> re-encode -> restore audio. Frames are streamed
through in overlapping batches rather than all loaded into memory at
once, so this scales to longer videos.
"""

from __future__ import annotations

import os
import tempfile
from dataclasses import dataclass, field
from typing import Iterable, Iterator, List, Optional

import numpy as np

from .detector import BoundingBox, detect_static_regions
from .inpaint import InpaintBackend
from .mask import boxes_to_mask
from .video_io import VideoMeta, mux_audio_and_finalize, open_frame_encoder, probe_video, read_frames, sample_frames


@dataclass
class OverlayRemovalResult:
    output_path: str
    boxes: List[BoundingBox]
    frame_count: int
    width: int
    height: int
    fps: float
    mask_coverage_pct: float
    meta: VideoMeta = field(repr=False)


def _batched_with_overlap(
    frame_iter: Iterable[np.ndarray], batch_size: int, overlap: int
) -> Iterator[tuple[List[np.ndarray], bool]]:
    """Group frames into batches, carrying `overlap` trailing frames from
    each batch into the next as temporal context for the inpainting
    backend. Yields (batch, is_first_batch)."""
    buffer: List[np.ndarray] = []
    is_first = True
    for frame in frame_iter:
        buffer.append(frame)
        if len(buffer) == batch_size:
            yield buffer, is_first
            buffer = buffer[-overlap:] if overlap > 0 else []
            is_first = False
    if buffer:
        yield buffer, is_first


class OverlayRemovalPipeline:
    """Orchestrates overlay detection, masking, inpainting and export.

    Args:
        backend: an `InpaintBackend` instance (e.g. `OpenCVInpaintBackend()`).
        dilate: pixels to grow the overlay mask by, to fully cover edges.
        feather: Gaussian blur radius (px) for soft mask edges / seamless blending.
        batch_size: frames processed per backend call.
        overlap: trailing frames carried between batches for temporal continuity.
    """

    def __init__(
        self,
        backend: InpaintBackend,
        dilate: int = 6,
        feather: int = 9,
        batch_size: int = 60,
        overlap: int = 4,
    ):
        self.backend = backend
        self.dilate = dilate
        self.feather = feather
        self.batch_size = batch_size
        self.overlap = overlap

    def run(
        self,
        input_path: str,
        output_path: str,
        boxes: Optional[List[BoundingBox]] = None,
        auto_detect: bool = False,
        detector_kwargs: Optional[dict] = None,
        sample_count: int = 24,
        codec: str = "h264",
        crf: int = 20,
    ) -> OverlayRemovalResult:
        """Run the full pipeline and write the result to `output_path`.

        Either pass `boxes` (regions you've identified yourself) or set
        `auto_detect=True` to let the detector propose regions from a
        sample of frames.
        """
        if not boxes and not auto_detect:
            raise ValueError("Provide `boxes` or set `auto_detect=True`.")

        meta = probe_video(input_path)

        if not boxes:
            samples = sample_frames(input_path, max_samples=sample_count)
            boxes = detect_static_regions(samples, **(detector_kwargs or {}))
            if not boxes:
                raise RuntimeError(
                    "No overlay regions were auto-detected. Try lowering "
                    "`variance_threshold` / `edge_density_threshold`, or "
                    "pass `boxes` explicitly."
                )

        mask = boxes_to_mask(meta.height, meta.width, boxes, dilate=self.dilate, feather=self.feather)
        coverage_pct = 100.0 * float(np.count_nonzero(mask)) / mask.size

        fd, video_only_path = tempfile.mkstemp(suffix=".mp4")
        os.close(fd)

        try:
            encoder = open_frame_encoder(
                video_only_path, meta.width, meta.height, meta.fps, codec=codec, crf=crf, meta=meta
            )
            assert encoder.stdin is not None

            frames_written = 0
            for batch, is_first in _batched_with_overlap(
                read_frames(input_path), self.batch_size, self.overlap
            ):
                processed = self.backend.process(batch, mask)
                skip = 0 if is_first else min(self.overlap, len(processed))
                for frame in processed[skip:]:
                    encoder.stdin.write(frame.astype(np.uint8).tobytes())
                    frames_written += 1

            encoder.stdin.close()
            ret = encoder.wait()
            if ret != 0:
                raise RuntimeError(f"ffmpeg encoder exited with status {ret}")

            mux_audio_and_finalize(video_only_path, input_path, output_path, meta.has_audio)
        finally:
            if os.path.exists(video_only_path):
                os.remove(video_only_path)

        return OverlayRemovalResult(
            output_path=output_path,
            boxes=boxes,
            frame_count=frames_written,
            width=meta.width,
            height=meta.height,
            fps=meta.fps,
            mask_coverage_pct=coverage_pct,
            meta=meta,
        )

    def preview_mask(self, input_path: str, boxes: List[BoundingBox]) -> np.ndarray:
        """Return the (feathered) mask for the given boxes, sized to the source video."""
        meta = probe_video(input_path)
        return boxes_to_mask(meta.height, meta.width, boxes, dilate=self.dilate, feather=self.feather)

    def suggest_boxes(self, input_path: str, sample_count: int = 24, **detector_kwargs) -> List[BoundingBox]:
        """Run auto-detection only, without processing the video. Useful for
        showing the user candidate regions in a UI before committing to a run."""
        samples = sample_frames(input_path, max_samples=sample_count)
        return detect_static_regions(samples, **detector_kwargs)
