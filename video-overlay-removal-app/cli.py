#!/usr/bin/env python3
"""Command-line interface for the video overlay removal pipeline.

Examples:
    # Auto-detect the overlay region and remove it:
    python cli.py input.mp4 output.mp4 --auto-detect

    # Remove one or more manually specified regions (x,y,w,h in pixels):
    python cli.py input.mp4 output.mp4 --box 1600,20,300,60 --box 0,0,120,40

    # Encode with H.265 at a higher quality (lower CRF = higher quality):
    python cli.py input.mp4 output.mp4 --auto-detect --codec h265 --crf 18
"""

from __future__ import annotations

import argparse
import sys

from overlay_removal.inpaint import get_backend
from overlay_removal.pipeline import OverlayRemovalPipeline


def _parse_box(value: str) -> tuple[int, int, int, int]:
    try:
        x, y, w, h = (int(v) for v in value.split(","))
    except ValueError as exc:
        raise argparse.ArgumentTypeError("--box must be 'x,y,w,h' (integers, comma-separated)") from exc
    return x, y, w, h


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Remove static overlays from a video you own.")
    parser.add_argument("input", help="Path to the source video.")
    parser.add_argument("output", help="Path to write the processed video.")

    region = parser.add_mutually_exclusive_group(required=True)
    region.add_argument(
        "--box",
        action="append",
        type=_parse_box,
        dest="boxes",
        help="Overlay region as x,y,w,h in pixels. Repeatable for multiple regions.",
    )
    region.add_argument(
        "--auto-detect",
        action="store_true",
        help="Automatically propose overlay region(s) from a sample of frames.",
    )

    parser.add_argument("--backend", default="opencv", choices=["opencv", "propainter", "e2fgvi", "sttn"],
                         help="Inpainting engine (default: opencv). The AI backends require separate setup; see README.")
    parser.add_argument("--codec", default="h264", choices=["h264", "h265"], help="Output video codec (default: h264).")
    parser.add_argument("--crf", type=int, default=20, help="Encoder quality, lower = higher quality/larger file (default: 20).")
    parser.add_argument("--dilate", type=int, default=6, help="Pixels to grow the mask by (default: 6).")
    parser.add_argument("--feather", type=int, default=9, help="Mask edge blur radius in pixels (default: 9).")
    parser.add_argument("--temporal-blend", type=float, default=0.4,
                         help="OpenCV backend only: 0-1 weight for blending with the optical-flow-warped previous frame (default: 0.4).")
    parser.add_argument("--sample-count", type=int, default=24, help="Frames sampled for auto-detection (default: 24).")

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    backend_kwargs = {"temporal_blend": args.temporal_blend} if args.backend == "opencv" else {}
    backend = get_backend(args.backend, **backend_kwargs)

    pipeline = OverlayRemovalPipeline(
        backend=backend,
        dilate=args.dilate,
        feather=args.feather,
    )

    try:
        result = pipeline.run(
            args.input,
            args.output,
            boxes=args.boxes,
            auto_detect=args.auto_detect,
            sample_count=args.sample_count,
            codec=args.codec,
            crf=args.crf,
        )
    except Exception as exc:  # surfaced as a clean CLI error, not a traceback
        print(f"error: {exc}", file=sys.stderr)
        return 1

    print(f"Wrote {result.output_path}")
    print(f"  resolution: {result.width}x{result.height} @ {result.fps:.2f} fps")
    print(f"  frames processed: {result.frame_count}")
    print(f"  overlay regions: {result.boxes}")
    print(f"  mask coverage: {result.mask_coverage_pct:.2f}% of frame area")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
