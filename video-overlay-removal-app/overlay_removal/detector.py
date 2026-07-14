"""Automatic detection of static overlay regions.

Overlays like camera timestamps, GPS burn-ins and logos share one trait
that the rest of the video usually doesn't: their pixels barely change
from frame to frame, even while the scene behind them moves. We exploit
that by measuring per-pixel variance across a sample of frames — low
variance + visible internal edges (i.e. not just a flat static
background, like sky) marks a region as a probable overlay.

This is a heuristic, not a classifier: it's meant to give the user a
good starting guess that they can accept or adjust, not a fully
automatic silver bullet.
"""

from __future__ import annotations

from typing import List, Tuple

import cv2
import numpy as np

BoundingBox = Tuple[int, int, int, int]  # (x, y, w, h)


def detect_static_regions(
    frames: List[np.ndarray],
    variance_threshold: float = 6.0,
    min_area_ratio: float = 0.00015,
    max_area_ratio: float = 0.20,
    edge_density_threshold: float = 0.06,
    merge_distance: int = 12,
) -> List[BoundingBox]:
    """Return candidate overlay bounding boxes found across a set of sample frames.

    Args:
        frames: representative frames sampled across the video (BGR).
        variance_threshold: max per-pixel intensity variance (0-255 scale)
            to be considered "static". Lower = stricter.
        min_area_ratio / max_area_ratio: candidate blob area, as a fraction
            of total frame area, allowed through. Filters out both noise
            speckles and overly large regions (e.g. a static background).
        edge_density_threshold: fraction of edge pixels required inside a
            candidate blob for it to be treated as text/graphics rather
            than a plain static surface.
        merge_distance: boxes closer than this (in pixels) are merged,
            since overlays like timestamps are often drawn as several
            disconnected glyphs.

    Returns:
        List of (x, y, w, h) boxes, largest first.
    """
    if len(frames) < 2:
        raise ValueError("Need at least 2 frames to detect static regions.")

    gray_stack = np.stack([cv2.cvtColor(f, cv2.COLOR_BGR2GRAY).astype(np.float32) for f in frames])
    variance_map = gray_stack.var(axis=0)
    mean_frame = gray_stack.mean(axis=0).astype(np.uint8)

    static_mask = (variance_map < variance_threshold).astype(np.uint8) * 255

    edges = cv2.Canny(mean_frame, 60, 150)
    kernel = np.ones((5, 5), np.uint8)
    edges_dilated = cv2.dilate(edges, kernel, iterations=1)

    candidate_mask = cv2.bitwise_and(static_mask, edges_dilated)

    close_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (merge_distance, merge_distance))
    candidate_mask = cv2.morphologyEx(candidate_mask, cv2.MORPH_CLOSE, close_kernel)

    contours, _ = cv2.findContours(candidate_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    frame_area = frames[0].shape[0] * frames[0].shape[1]
    min_area = frame_area * min_area_ratio
    max_area = frame_area * max_area_ratio

    boxes: List[BoundingBox] = []
    for c in contours:
        x, y, w, h = cv2.boundingRect(c)
        area = w * h
        if area < min_area or area > max_area:
            continue

        roi_edges = edges[y : y + h, x : x + w]
        if roi_edges.size == 0:
            continue
        density = np.count_nonzero(roi_edges) / roi_edges.size
        if density < edge_density_threshold:
            continue

        boxes.append((x, y, w, h))

    boxes.sort(key=lambda b: b[2] * b[3], reverse=True)
    return boxes
