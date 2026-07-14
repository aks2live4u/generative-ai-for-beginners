import numpy as np

from overlay_removal.mask import binary_mask, boxes_to_mask, mask_bounds


def test_boxes_to_mask_covers_box_region():
    mask = boxes_to_mask(100, 100, [(10, 10, 20, 20)], dilate=0, feather=0)
    assert mask.shape == (100, 100)
    assert mask[15, 15] == 255
    assert mask[0, 0] == 0


def test_boxes_to_mask_dilate_grows_region():
    tight = boxes_to_mask(100, 100, [(40, 40, 10, 10)], dilate=0, feather=0)
    grown = boxes_to_mask(100, 100, [(40, 40, 10, 10)], dilate=5, feather=0)
    assert np.count_nonzero(grown) > np.count_nonzero(tight)


def test_boxes_to_mask_feather_creates_soft_edge():
    mask = boxes_to_mask(100, 100, [(40, 40, 20, 20)], dilate=0, feather=9)
    # center should be fully opaque, but somewhere near the edge should be partial
    assert mask[50, 50] == 255
    edge_values = mask[40, :]
    assert np.any((edge_values > 0) & (edge_values < 255))


def test_binary_mask_thresholds():
    soft = np.array([[0, 100, 200, 255]], dtype=np.uint8)
    hard = binary_mask(soft, threshold=127)
    assert list(hard[0]) == [0, 0, 255, 255]


def test_mask_bounds_empty():
    mask = np.zeros((50, 50), dtype=np.uint8)
    assert mask_bounds(mask) is None


def test_mask_bounds_matches_box():
    mask = boxes_to_mask(100, 100, [(10, 20, 30, 40)], dilate=0, feather=0)
    x, y, w, h = mask_bounds(mask)
    assert (x, y, w, h) == (10, 20, 30, 40)
