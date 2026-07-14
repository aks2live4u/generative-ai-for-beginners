import numpy as np

from overlay_removal.detector import detect_static_regions


def _make_synthetic_frames(n=15, size=200, seed=0):
    """Frames with a random-noise moving background and a fixed
    "timestamp"-like overlay patch (a dark plate with bright digit-segment
    bars) in one corner, constant across every frame."""
    rng = np.random.default_rng(seed)
    overlay_box = (150, 10, 40, 20)  # x, y, w, h
    ox, oy, ow, oh = overlay_box

    plate = np.full((oh, ow, 3), 20, dtype=np.uint8)
    plate[4:8, 4:34] = (230, 230, 230)
    plate[12:16, 4:34] = (230, 230, 230)

    frames = []
    for _ in range(n):
        frame = rng.integers(0, 255, size=(size, size, 3), dtype=np.uint8)
        frame[oy : oy + oh, ox : ox + ow] = plate
        frames.append(frame)
    return frames, overlay_box


def test_detect_static_regions_finds_overlay():
    frames, overlay_box = _make_synthetic_frames()
    boxes = detect_static_regions(frames)

    assert len(boxes) > 0

    ox, oy, ow, oh = overlay_box
    overlay_center = (ox + ow / 2, oy + oh / 2)

    def contains(box, point):
        x, y, w, h = box
        px, py = point
        return x <= px <= x + w and y <= py <= y + h

    assert any(contains(b, overlay_center) for b in boxes)


def test_detect_static_regions_requires_multiple_frames():
    frames, _ = _make_synthetic_frames(n=1)
    try:
        detect_static_regions(frames)
        assert False, "expected ValueError"
    except ValueError:
        pass
