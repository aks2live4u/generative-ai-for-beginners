"""Pluggable video inpainting backends.

`InpaintBackend` is the interface the pipeline talks to. Two concerns are
handled by every backend:

1. Fill the masked region with plausible content (spatial inpainting).
2. Keep that fill consistent from frame to frame so it doesn't flicker
   (temporal consistency) — the hard part of *video* inpainting versus
   single-image inpainting.

`OpenCVInpaintBackend` is a real, working, CPU-only baseline: per-frame
Telea/Navier-Stokes inpainting plus optical-flow-guided temporal
blending. It has no GPU or model-weight dependency, so it runs anywhere
OpenCV runs — good for short clips and simple overlays.

For harder cases (large occlusions, complex motion behind the overlay)
you'll get noticeably better, more temporally-stable results from a
learned video inpainting model. `ProPainterBackend`, `E2FGVIBackend` and
`STTNBackend` below are documented extension points: they define the
exact interface to implement against the official ProPainter / E2FGVI /
STTN repos (each needs PyTorch + pretrained weights, which are too large
to vendor here). Swap one in via `get_backend(...)` once implemented.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import List

import cv2
import numpy as np

from .mask import binary_mask


class InpaintBackend(ABC):
    """Base interface for a video inpainting engine."""

    #: Whether this backend benefits from being given overlapping frame
    #: batches (extra context frames before/after) for temporal consistency.
    requires_temporal_context: bool = False

    @abstractmethod
    def process(self, frames: List[np.ndarray], mask: np.ndarray) -> List[np.ndarray]:
        """Return a new list of frames with the masked region reconstructed.

        Args:
            frames: consecutive BGR frames (a batch/window of the video).
            mask: single-channel mask, same H/W as the frames. Values are
                treated as an alpha blend weight (0 = keep original pixel,
                255 = fully replaced by inpainted content), so callers can
                pass a feathered mask for seamless blending.
        """
        raise NotImplementedError


class OpenCVInpaintBackend(InpaintBackend):
    """Baseline backend: per-frame OpenCV inpainting + optical-flow temporal blend.

    Args:
        method: "telea" (cv2.INPAINT_TELEA) or "ns" (Navier-Stokes).
        radius: inpainting neighborhood radius in pixels.
        temporal_blend: 0-1 weight given to the optical-flow-warped
            previous reconstruction when blending with the current
            frame's fresh inpaint result. 0 disables temporal blending
            (independent per-frame inpainting, more flicker-prone).
            Higher values are smoother across frames but can smear
            content that's genuinely changing behind the overlay.
    """

    _METHODS = {"telea": cv2.INPAINT_TELEA, "ns": cv2.INPAINT_NS}

    def __init__(self, method: str = "telea", radius: int = 5, temporal_blend: float = 0.4):
        if method not in self._METHODS:
            raise ValueError(f"method must be one of {sorted(self._METHODS)}")
        self.flag = self._METHODS[method]
        self.radius = radius
        self.temporal_blend = float(np.clip(temporal_blend, 0.0, 1.0))

    def process(self, frames: List[np.ndarray], mask: np.ndarray) -> List[np.ndarray]:
        if not frames:
            return []

        hard = binary_mask(mask)
        soft = (mask.astype(np.float32) / 255.0)[..., None]

        h, w = mask.shape[:2]
        grid_x, grid_y = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))

        outputs: List[np.ndarray] = []
        prev_inpainted = None
        prev_gray = None

        for frame in frames:
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            inpainted = cv2.inpaint(frame, hard, self.radius, self.flag)

            if prev_inpainted is not None and self.temporal_blend > 0:
                flow = cv2.calcOpticalFlowFarneback(
                    prev_gray, gray, None, 0.5, 3, 15, 3, 5, 1.2, 0
                )
                map_x = grid_x + flow[..., 0]
                map_y = grid_y + flow[..., 1]
                warped_prev = cv2.remap(
                    prev_inpainted, map_x, map_y, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE
                )
                blended_patch = cv2.addWeighted(
                    warped_prev, self.temporal_blend, inpainted, 1 - self.temporal_blend, 0
                )
            else:
                blended_patch = inpainted

            output = frame.astype(np.float32) * (1 - soft) + blended_patch.astype(np.float32) * soft
            output = np.clip(output, 0, 255).astype(np.uint8)

            outputs.append(output)
            prev_inpainted = blended_patch
            prev_gray = gray

        return outputs


class _UnimplementedModelBackend(InpaintBackend):
    """Common scaffold for AI video-inpainting model integrations.

    These models produce much better temporally-consistent results than
    the OpenCV baseline, especially for large or moving occlusions, but
    require PyTorch plus multi-hundred-MB pretrained checkpoints that
    aren't vendored in this repo. Subclasses document exactly what to
    wire up.
    """

    requires_temporal_context = True
    _repo_url = ""
    _name = "model"

    def __init__(self, weights_path: str | None = None, device: str = "cuda"):
        self.weights_path = weights_path
        self.device = device

    def process(self, frames: List[np.ndarray], mask: np.ndarray) -> List[np.ndarray]:
        raise NotImplementedError(
            f"{self._name} is not vendored in this repo (it needs PyTorch + "
            f"pretrained weights). To use it: clone {self._repo_url}, load the "
            "model in this method's place, run it on `frames` (a list of BGR "
            "np.ndarray, all same shape) with `mask` marking the region to "
            "reconstruct, and return the same-length list of reconstructed "
            "BGR frames. Because these models look at neighboring frames for "
            "temporal context, request overlapping batches from the pipeline "
            "via `requires_temporal_context = True` (already set)."
        )


class ProPainterBackend(_UnimplementedModelBackend):
    """Integration point for ProPainter (flow-guided video inpainting)."""

    _repo_url = "https://github.com/sczhou/ProPainter"
    _name = "ProPainter"


class E2FGVIBackend(_UnimplementedModelBackend):
    """Integration point for E2FGVI (End-to-End Flow-Guided Video Inpainting)."""

    _repo_url = "https://github.com/MCG-NKU/E2FGVI"
    _name = "E2FGVI"


class STTNBackend(_UnimplementedModelBackend):
    """Integration point for STTN (Spatial-Temporal Transformer Network)."""

    _repo_url = "https://github.com/researchmm/STTN"
    _name = "STTN"


BACKEND_REGISTRY = {
    "opencv": OpenCVInpaintBackend,
    "propainter": ProPainterBackend,
    "e2fgvi": E2FGVIBackend,
    "sttn": STTNBackend,
}


def get_backend(name: str, **kwargs) -> InpaintBackend:
    """Instantiate a backend by name. See BACKEND_REGISTRY for options."""
    key = name.lower()
    if key not in BACKEND_REGISTRY:
        raise ValueError(f"Unknown backend '{name}'. Choose from {sorted(BACKEND_REGISTRY)}.")
    return BACKEND_REGISTRY[key](**kwargs)
