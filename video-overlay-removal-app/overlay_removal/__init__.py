"""Video overlay removal toolkit.

A small, dependency-light pipeline for removing static overlays you control
(timestamps, GPS burn-ins, your own logos/captions, recording UI, sensor
blemishes, etc.) from your own videos using classical inpainting, with a
pluggable backend interface for swapping in AI video-inpainting models
such as ProPainter, E2FGVI or STTN for better temporal consistency.
"""

from .pipeline import OverlayRemovalPipeline

__all__ = ["OverlayRemovalPipeline"]
