"""Streamlit UI for the video overlay removal pipeline.

Run with:
    streamlit run app.py

Workflow: upload a video you own -> preview the first frame -> either let
the app auto-detect the overlay region or draw box(es) manually via
numeric fields -> preview the mask -> run the pipeline -> download the
result.
"""

from __future__ import annotations

import os
import tempfile

import cv2
import numpy as np
import streamlit as st

from overlay_removal.detector import detect_static_regions
from overlay_removal.inpaint import OpenCVInpaintBackend
from overlay_removal.mask import boxes_to_mask
from overlay_removal.pipeline import OverlayRemovalPipeline
from overlay_removal.video_io import probe_video, sample_frames

st.set_page_config(page_title="Video Overlay Removal", layout="wide")

st.title("Video Overlay Removal")
st.caption(
    "Remove static overlays you control from your own videos — camera timestamps, "
    "GPS burn-ins, your own captions/logos, recording UI, sensor blemishes — using "
    "AI-assisted video inpainting with temporal consistency."
)

if "source_path" not in st.session_state:
    st.session_state.source_path = None
if "boxes" not in st.session_state:
    st.session_state.boxes = []

uploaded = st.file_uploader("Upload a video", type=["mp4", "mov", "mkv", "avi"])

if uploaded is not None:
    if st.session_state.get("uploaded_name") != uploaded.name:
        suffix = os.path.splitext(uploaded.name)[1] or ".mp4"
        fd, path = tempfile.mkstemp(suffix=suffix)
        with os.fdopen(fd, "wb") as f:
            f.write(uploaded.getbuffer())
        st.session_state.source_path = path
        st.session_state.uploaded_name = uploaded.name
        st.session_state.boxes = []

if st.session_state.source_path:
    src = st.session_state.source_path
    meta = probe_video(src)
    st.write(
        f"**{meta.width}x{meta.height}** @ **{meta.fps:.2f} fps**, "
        f"{meta.frame_count} frames (~{meta.duration:.1f}s), "
        f"audio: {'yes' if meta.has_audio else 'no'}"
    )

    first_frame = next(iter(sample_frames(src, max_samples=1)))
    frame_rgb = cv2.cvtColor(first_frame, cv2.COLOR_BGR2RGB)

    col_controls, col_preview = st.columns([1, 1.4])

    with col_controls:
        st.subheader("1. Define the overlay region(s)")

        if st.button("Auto-detect overlay region(s)"):
            samples = sample_frames(src, max_samples=24)
            detected = detect_static_regions(samples)
            st.session_state.boxes = detected
            if not detected:
                st.warning("No static overlay was detected automatically. Add a region manually below.")

        st.markdown("**Manual region** (pixels, top-left origin)")
        mx = st.number_input("x", min_value=0, max_value=meta.width, value=0, key="mx")
        my = st.number_input("y", min_value=0, max_value=meta.height, value=0, key="my")
        mw = st.number_input("width", min_value=1, max_value=meta.width, value=min(200, meta.width), key="mw")
        mh = st.number_input("height", min_value=1, max_value=meta.height, value=min(60, meta.height), key="mh")

        add_col, clear_col = st.columns(2)
        if add_col.button("Add region"):
            st.session_state.boxes = st.session_state.boxes + [(int(mx), int(my), int(mw), int(mh))]
        if clear_col.button("Clear all regions"):
            st.session_state.boxes = []

        if st.session_state.boxes:
            st.write("Current regions:", st.session_state.boxes)

        st.subheader("2. Inpainting & export settings")
        temporal_blend = st.slider(
            "Temporal smoothing (reduces flicker)", 0.0, 0.9, 0.4, 0.05,
            help="Blends each frame's reconstruction with the optical-flow-warped "
                 "previous frame. Higher = smoother across frames but can smear fast motion.",
        )
        codec = st.selectbox("Codec", ["h264", "h265"], index=0)
        crf = st.slider("Quality (CRF, lower = better)", 14, 30, 20)
        dilate = st.slider("Mask growth (px)", 0, 20, 6)
        feather = st.slider("Mask feather (px)", 0, 25, 9)

    with col_preview:
        st.subheader("Preview")
        preview = frame_rgb.copy()
        for x, y, w, h in st.session_state.boxes:
            cv2.rectangle(preview, (x, y), (x + w, y + h), (255, 0, 0), 2)
        st.image(preview, caption="First frame with overlay region(s) outlined", use_container_width=True)

        if st.session_state.boxes:
            mask = boxes_to_mask(meta.height, meta.width, st.session_state.boxes, dilate=dilate, feather=feather)
            st.image(mask, caption="Mask (white = will be reconstructed)", use_container_width=True, clamp=True)

    st.subheader("3. Run")
    if st.button("Remove overlay", type="primary", disabled=not st.session_state.boxes):
        out_fd, out_path = tempfile.mkstemp(suffix=".mp4")
        os.close(out_fd)

        backend = OpenCVInpaintBackend(temporal_blend=temporal_blend)
        pipeline = OverlayRemovalPipeline(backend=backend, dilate=dilate, feather=feather)

        with st.spinner("Processing video... this can take a while for longer clips."):
            result = pipeline.run(
                src, out_path,
                boxes=st.session_state.boxes,
                codec=codec, crf=crf,
            )

        st.success(f"Done — {result.frame_count} frames processed.")
        st.video(result.output_path)
        with open(result.output_path, "rb") as f:
            st.download_button("Download result", f, file_name="overlay_removed.mp4", mime="video/mp4")
else:
    st.info("Upload a video to get started.")
