"""
test_augmentor.py — Unit tests for pixel-safe augmentation operations.
"""

import numpy as np
import pytest
from PIL import Image

from src.augmentor import (
    adjust_saturation,
    adjust_value,
    augment_single,
    flip_horizontal,
    palette_swap,
    scale,
    shift_hue,
    translate,
)
from src.config import AugmentationConfig


# ── Helpers ──────────────────────────────────────────────────────────────────

def _make_test_sprite(w: int = 8, h: int = 8) -> Image.Image:
    """Create a small RGBA test sprite with known pixel values."""
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    # Red block top-left
    arr[0:4, 0:4] = [255, 0, 0, 255]
    # Blue block top-right
    arr[0:4, 4:8] = [0, 0, 255, 255]
    # Green block bottom-left
    arr[4:8, 0:4] = [0, 255, 0, 255]
    # White block bottom-right
    arr[4:8, 4:8] = [255, 255, 255, 255]
    return Image.fromarray(arr, "RGBA")


# ── Tests ────────────────────────────────────────────────────────────────────

class TestScale:
    def test_identity(self):
        img = _make_test_sprite()
        out = scale(img, 1)
        assert out.size == img.size
        assert np.array_equal(np.array(out), np.array(img))

    def test_2x(self):
        img = _make_test_sprite(8, 8)
        out = scale(img, 2)
        assert out.size == (16, 16)
        # Each pixel should be replicated 2×2
        arr = np.array(out)
        assert np.array_equal(arr[0, 0], arr[0, 1])
        assert np.array_equal(arr[0, 0], arr[1, 0])

    def test_3x(self):
        img = _make_test_sprite(4, 4)
        out = scale(img, 3)
        assert out.size == (12, 12)

    def test_invalid(self):
        img = _make_test_sprite()
        with pytest.raises(ValueError):
            scale(img, 0)


class TestShiftHue:
    def test_zero_shift(self):
        img = _make_test_sprite()
        out = shift_hue(img, 0)
        assert out.size == img.size

    def test_full_rotation(self):
        img = _make_test_sprite()
        out = shift_hue(img, 360)
        # Full rotation = approximately back to original
        arr_orig = np.array(img.convert("RGB"))
        arr_out = np.array(out.convert("RGB"))
        assert np.allclose(arr_orig, arr_out, atol=2)

    def test_preserves_alpha(self):
        img = _make_test_sprite()
        out = shift_hue(img, 45)
        alpha_orig = np.array(img)[:, :, 3]
        alpha_out = np.array(out)[:, :, 3]
        assert np.array_equal(alpha_orig, alpha_out)


class TestAdjustSaturation:
    def test_identity(self):
        img = _make_test_sprite()
        out = adjust_saturation(img, 1.0)
        arr_orig = np.array(img.convert("RGB"))
        arr_out = np.array(out.convert("RGB"))
        assert np.allclose(arr_orig, arr_out, atol=1)

    def test_desaturate(self):
        img = _make_test_sprite()
        out = adjust_saturation(img, 0.0)
        # All saturation removed → greyscale
        arr = np.array(out.convert("RGB"))
        # For greyscale, R == G == B
        assert np.all(arr[:, :, 0] == arr[:, :, 1])
        assert np.all(arr[:, :, 1] == arr[:, :, 2])


class TestAdjustValue:
    def test_identity(self):
        img = _make_test_sprite()
        out = adjust_value(img, 1.0)
        arr_orig = np.array(img.convert("RGB"))
        arr_out = np.array(out.convert("RGB"))
        assert np.allclose(arr_orig, arr_out, atol=1)

    def test_darken(self):
        img = _make_test_sprite()
        out = adjust_value(img, 0.5)
        arr_orig = np.array(img.convert("RGB")).astype(float)
        arr_out = np.array(out.convert("RGB")).astype(float)
        # Generally darker
        assert arr_out.mean() < arr_orig.mean()


class TestTranslate:
    def test_no_shift(self):
        img = _make_test_sprite()
        out = translate(img, 0, 0)
        assert out.size == img.size

    def test_shift_right(self):
        img = _make_test_sprite()
        out = translate(img, 4, 0, bg_color=(0, 0, 0, 0))
        arr = np.array(out)
        # Left 4 columns should be bg (transparent)
        assert np.all(arr[:, :4, 3] == 0)

    def test_dimensions_preserved(self):
        img = _make_test_sprite()
        out = translate(img, 2, 3)
        assert out.size == img.size


class TestFlipHorizontal:
    def test_flip(self):
        img = _make_test_sprite()
        out = flip_horizontal(img)
        arr_orig = np.array(img)
        arr_out = np.array(out)
        # First column of flipped == last column of original
        assert np.array_equal(arr_out[:, 0], arr_orig[:, -1])

    def test_double_flip(self):
        img = _make_test_sprite()
        out = flip_horizontal(flip_horizontal(img))
        assert np.array_equal(np.array(out), np.array(img))


class TestPaletteSwap:
    def test_single_swap(self):
        img = _make_test_sprite()
        out = palette_swap(img, {"#ff0000": "#00ff00"})
        arr = np.array(out)
        # Top-left block was red, should now be green
        assert np.all(arr[0:4, 0:4, 0] == 0)
        assert np.all(arr[0:4, 0:4, 1] == 255)

    def test_no_match(self):
        img = _make_test_sprite()
        out = palette_swap(img, {"#abcdef": "#000000"})
        assert np.array_equal(np.array(out), np.array(img))


class TestAugmentPipeline:
    def test_produces_image(self):
        img = _make_test_sprite()
        cfg = AugmentationConfig(
            num_variants=1,
            scale=[1],
            hue_shift=(0, 0),
            saturation=(1.0, 1.0),
            value=(1.0, 1.0),
            translate_x=(0, 0),
            translate_y=(0, 0),
            flip_horizontal=False,
        )
        out = augment_single(img, cfg)
        assert isinstance(out, Image.Image)
        assert out.size == img.size
