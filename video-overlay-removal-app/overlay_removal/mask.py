"""Mask construction from bounding boxes.

The mask marks which pixels are the unwanted overlay (255 = remove &
reconstruct, 0 = keep). We dilate it slightly so we don't leave a thin
halo of overlay pixels at the edges, and feather it so the inpainted
patch blends into the surrounding video instead of showing a hard seam.
"""

from __future__ import annotations

from typing import Iterable, Tuple

import cv2
import numpy as np

BoundingBox = Tuple[int, int, int, int]  # (x, y, w, h)


def boxes_to_mask(
    height: int,
    width: int,
    boxes: Iterable[BoundingBox],
    dilate: int = 6,
    feather: int = 9,
) -> np.ndarray:
    """Build a single-channel uint8 mask (0 or 255) covering the given boxes.

    Args:
        height, width: mask/frame dimensions.
        boxes: (x, y, w, h) regions to mark for removal.
        dilate: pixels to grow each box by, so overlay edges are fully covered.
        feather: Gaussian blur radius (px) applied to soften the mask edge
            for blending. Set to 0 to disable.
    """
    mask = np.zeros((height, width), dtype=np.uint8)
    for x, y, w, h in boxes:
        x0, y0 = max(0, x), max(0, y)
        x1, y1 = min(width, x + w), min(height, y + h)
        if x1 > x0 and y1 > y0:
            mask[y0:y1, x0:x1] = 255

    if dilate > 0:
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (dilate * 2 + 1, dilate * 2 + 1))
        mask = cv2.dilate(mask, kernel)

    if feather > 0:
        blur_size = feather * 2 + 1
        mask = cv2.GaussianBlur(mask, (blur_size, blur_size), 0)

    return mask


def binary_mask(mask: np.ndarray, threshold: int = 127) -> np.ndarray:
    """Collapse a feathered mask back to strict 0/255, e.g. for algorithms
    (like cv2.inpaint) that require a hard mask rather than soft alpha."""
    return np.where(mask >= threshold, 255, 0).astype(np.uint8)


def mask_bounds(mask: np.ndarray) -> BoundingBox | None:
    """Return the bounding box enclosing all non-zero mask pixels, or None if empty."""
    ys, xs = np.nonzero(mask)
    if len(xs) == 0:
        return None
    x0, x1 = xs.min(), xs.max()
    y0, y1 = ys.min(), ys.max()
    return int(x0), int(y0), int(x1 - x0 + 1), int(y1 - y0 + 1)
