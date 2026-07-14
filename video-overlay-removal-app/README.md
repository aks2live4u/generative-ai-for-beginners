# Video Overlay Removal

A sample app for removing static overlays **you control** from **your own
videos** — camera timestamps, date/time stamps, GPS coordinates, captions
you added, logos from your own exports, recording UI, sensor artifacts or
static blemishes — and reconstructing the hidden pixels with AI-assisted
video inpainting.

> This tool is for restoring/cleaning up content you own or have the
> rights to edit. It is not intended for removing watermarks or overlays
> from other people's copyrighted or trademarked content.

## How it works

The pipeline mirrors how professional overlay/watermark removal tools work:

1. **Import** the video (`overlay_removal/video_io.py` probes resolution,
   frame rate, color metadata and audio via `ffprobe`).
2. **Detect** the overlay — either automatically, by finding regions that
   stay pixel-static across frames while the rest of the scene changes
   (`overlay_removal/detector.py`), or by letting the user specify a
   rectangle directly.
3. **Build a mask** of the region to remove, dilated slightly to fully
   cover the overlay's edges and feathered so the reconstruction blends in
   (`overlay_removal/mask.py`).
4. **Inpaint** the masked region frame-by-frame with temporal consistency
   so the fill doesn't flicker (`overlay_removal/inpaint.py`).
5. **Re-encode** to H.264/H.265, preserving the original resolution, frame
   rate and color metadata, then **mux the original audio track back in**
   untouched (`overlay_removal/video_io.py`).

## Inpainting backends

- **`opencv` (default, included, no GPU needed)** — per-frame OpenCV
  Telea/Navier-Stokes inpainting, blended across frames using
  optical-flow-warped context from the previous frame to reduce flicker.
  Works out of the box, good for small/simple overlays (timestamps,
  small logos) on modest hardware.
- **`propainter` / `e2fgvi` / `sttn`** — integration points for
  [ProPainter](https://github.com/sczhou/ProPainter),
  [E2FGVI](https://github.com/MCG-NKU/E2FGVI) and
  [STTN](https://github.com/researchmm/STTN), the learned
  flow-guided/transformer video inpainting models referenced in the brief.
  These give noticeably better results on large or moving occlusions, but
  need PyTorch and multi-hundred-MB pretrained weights that aren't
  vendored in this repo. `overlay_removal/inpaint.py` defines the exact
  interface (`InpaintBackend.process(frames, mask) -> frames`) to
  implement against the official repos — see the docstrings on
  `ProPainterBackend`, `E2FGVIBackend` and `STTNBackend`.

Add your own backend by subclassing `InpaintBackend` and registering it in
`BACKEND_REGISTRY`.

## Setup

```bash
cd video-overlay-removal-app
pip install -r requirements.txt
```

You also need `ffmpeg`/`ffprobe` on your `PATH` (used for reading
metadata, encoding, and audio muxing):

```bash
# Debian/Ubuntu
sudo apt install ffmpeg
# macOS
brew install ffmpeg
```

## Usage

### Web UI

```bash
streamlit run app.py
```

Upload a video, either click **Auto-detect overlay region(s)** or enter a
rectangle manually, preview the mask, then run the removal and download
the result.

### CLI

```bash
# Auto-detect the overlay region
python cli.py input.mp4 output.mp4 --auto-detect

# Or specify region(s) explicitly: x,y,w,h in pixels, repeatable
python cli.py input.mp4 output.mp4 --box 1600,20,300,60

# Higher quality H.265 output
python cli.py input.mp4 output.mp4 --auto-detect --codec h265 --crf 18
```

Run `python cli.py --help` for all options (mask dilation/feathering,
temporal smoothing strength, sample count for auto-detection, etc.).

### Library

```python
from overlay_removal.inpaint import OpenCVInpaintBackend
from overlay_removal.pipeline import OverlayRemovalPipeline

pipeline = OverlayRemovalPipeline(backend=OpenCVInpaintBackend(temporal_blend=0.4))
result = pipeline.run("input.mp4", "output.mp4", boxes=[(1600, 20, 300, 60)])
print(result.frame_count, result.mask_coverage_pct)
```

## Limitations

- The auto-detector is a heuristic (per-pixel variance + edge density), not
  a trained classifier — it's tuned for text/graphic overlays (timestamps,
  logos, captions) rather than large plain-color regions, and its
  suggestions should be reviewed before running the full pipeline.
- The bundled `opencv` backend is CPU-only and per-frame; for large,
  moving, or long-duration occlusions, wire up one of the AI backends
  above for meaningfully better temporal consistency.
- HDR mastering metadata (mastering display / content light level) is
  best-effort: basic color range/space/transfer/primaries tags are
  forwarded to the encoder, but full HDR side-data preservation depends on
  your installed ffmpeg/libx265 build.

## Project layout

```
video-overlay-removal-app/
├── app.py                     # Streamlit web UI
├── cli.py                     # Command-line interface
├── overlay_removal/
│   ├── video_io.py            # ffprobe metadata, frame I/O, encoding, audio mux
│   ├── detector.py            # automatic static-overlay region detection
│   ├── mask.py                # bounding boxes -> dilated/feathered mask
│   ├── inpaint.py             # inpainting backend interface + OpenCV baseline
│   └── pipeline.py            # end-to-end orchestration
└── tests/                     # unit tests + an end-to-end smoke test
```

## Tests

```bash
pytest tests/
```
