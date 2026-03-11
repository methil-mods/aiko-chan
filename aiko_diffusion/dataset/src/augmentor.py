"""
augmentor.py — Pixel-safe image augmentation pipeline.

Every operation uses nearest-neighbor interpolation and integer arithmetic
to preserve the hard pixel edges of pixel art sprites.
"""

from __future__ import annotations

import pathlib
import random
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List, Optional, Tuple

from tqdm import tqdm

import numpy as np
from PIL import Image

from .config import AugmentationConfig


# ── Individual augmentation operations ───────────────────────────────────────

def scale(img: Image.Image, factor: int) -> Image.Image:
    """Integer nearest-neighbor scale (e.g. 2× → each pixel becomes 2×2)."""
    if factor < 1:
        raise ValueError(f"Scale factor must be >= 1, got {factor}")
    if factor == 1:
        return img.copy()
    new_size = (img.width * factor, img.height * factor)
    return img.resize(new_size, resample=Image.NEAREST)


def shift_hue(img: Image.Image, degrees: float) -> Image.Image:
    """Rotate the hue channel by *degrees* (−180 … +180).

    Works on RGB / RGBA images.  Alpha channel is preserved.
    """
    has_alpha = img.mode == "RGBA"
    alpha = img.split()[-1] if has_alpha else None

    rgb = img.convert("RGB")
    arr = np.array(rgb, dtype=np.float32)

    # RGB → HSV (H in [0, 360), S/V in [0, 1])
    arr_norm = arr / 255.0
    hsv = _rgb_to_hsv(arr_norm)
    hsv[..., 0] = (hsv[..., 0] + degrees / 360.0) % 1.0
    rgb_out = (_hsv_to_rgb(hsv) * 255.0).clip(0, 255).astype(np.uint8)

    result = Image.fromarray(rgb_out, "RGB")
    if has_alpha and alpha is not None:
        result.putalpha(alpha)
    return result


def adjust_saturation(img: Image.Image, factor: float) -> Image.Image:
    """Multiply the saturation channel by *factor* (clamp to [0, 1])."""
    has_alpha = img.mode == "RGBA"
    alpha = img.split()[-1] if has_alpha else None

    rgb = img.convert("RGB")
    arr = np.array(rgb, dtype=np.float32) / 255.0
    hsv = _rgb_to_hsv(arr)
    hsv[..., 1] = np.clip(hsv[..., 1] * factor, 0.0, 1.0)
    rgb_out = (_hsv_to_rgb(hsv) * 255.0).clip(0, 255).astype(np.uint8)

    result = Image.fromarray(rgb_out, "RGB")
    if has_alpha and alpha is not None:
        result.putalpha(alpha)
    return result


def adjust_value(img: Image.Image, factor: float) -> Image.Image:
    """Multiply the value (brightness) channel by *factor* (clamp to [0, 1])."""
    has_alpha = img.mode == "RGBA"
    alpha = img.split()[-1] if has_alpha else None

    rgb = img.convert("RGB")
    arr = np.array(rgb, dtype=np.float32) / 255.0
    hsv = _rgb_to_hsv(arr)
    hsv[..., 2] = np.clip(hsv[..., 2] * factor, 0.0, 1.0)
    rgb_out = (_hsv_to_rgb(hsv) * 255.0).clip(0, 255).astype(np.uint8)

    result = Image.fromarray(rgb_out, "RGB")
    if has_alpha and alpha is not None:
        result.putalpha(alpha)
    return result


def translate(
    img: Image.Image,
    dx: int,
    dy: int,
    bg_color: Tuple[int, ...] = (0, 0, 0, 0),
) -> Image.Image:
    """Shift the image by (*dx*, *dy*) pixels, filling with *bg_color*."""
    mode = img.mode
    if mode not in ("RGB", "RGBA"):
        img = img.convert("RGBA")
        mode = "RGBA"

    bg = bg_color[:len(mode)]  # match channel count
    canvas = Image.new(mode, img.size, bg)
    canvas.paste(img, (dx, dy))
    return canvas


def flip_horizontal(img: Image.Image) -> Image.Image:
    """Mirror the image horizontally."""
    return img.transpose(Image.FLIP_LEFT_RIGHT)


def palette_swap(
    img: Image.Image, color_map: Dict[str, str]
) -> Image.Image:
    """Remap exact pixel colors according to *color_map*.

    Keys and values are hex color strings like ``"#ff0000"``.
    Works on RGBA images — alpha is compared/replaced too if 8-char hex is
    given, otherwise only RGB channels are swapped for opaque pixels.
    """
    arr = np.array(img)
    for src_hex, dst_hex in color_map.items():
        src = _hex_to_tuple(src_hex)
        dst = _hex_to_tuple(dst_hex)
        channels = min(len(src), arr.shape[2])
        mask = np.all(arr[..., :channels] == src[:channels], axis=-1)
        arr[mask, :channels] = dst[:channels]
    return Image.fromarray(arr, img.mode)


# ── Pipeline ─────────────────────────────────────────────────────────────────

def augment_single(img: Image.Image, cfg: AugmentationConfig) -> Image.Image:
    """Apply a random combination of augmentations to a single image."""
    result = img.copy()

    # Scale — pick a random factor from the allowed list
    factor = random.choice(cfg.scale)
    if factor != 1:
        result = scale(result, factor)

    # Hue shift
    degrees = random.uniform(*cfg.hue_shift)
    if abs(degrees) > 0.5:
        result = shift_hue(result, degrees)

    # Saturation
    sat_factor = random.uniform(*cfg.saturation)
    if abs(sat_factor - 1.0) > 0.01:
        result = adjust_saturation(result, sat_factor)

    # Value / brightness
    val_factor = random.uniform(*cfg.value)
    if abs(val_factor - 1.0) > 0.01:
        result = adjust_value(result, val_factor)

    # Translation
    dx = random.randint(*cfg.translate_x)
    dy = random.randint(*cfg.translate_y)
    if dx != 0 or dy != 0:
        result = translate(result, dx, dy, bg_color=cfg.bg_color)

    # Horizontal flip
    if cfg.flip_horizontal and random.random() > 0.5:
        result = flip_horizontal(result)

    # Palette swap
    if cfg.palette_swap:
        result = palette_swap(result, cfg.palette_swap)

    return result


def _process_single_source(
    src_path: pathlib.Path,
    output_dir: pathlib.Path,
    cfg: AugmentationConfig,
    variants: int,
) -> List[pathlib.Path]:
    """Augment one source image and return paths to created files."""
    img = Image.open(src_path).convert("RGBA")
    stem = src_path.stem
    created: List[pathlib.Path] = []
    for i in range(variants):
        aug = augment_single(img, cfg)
        out_name = f"{stem}_aug{i:04d}.png"
        out_path = output_dir / out_name
        aug.save(out_path)
        created.append(out_path)
    return created


def augment_dataset(
    input_dir: str | pathlib.Path,
    output_dir: str | pathlib.Path,
    cfg: AugmentationConfig,
    num_variants: Optional[int] = None,
    workers: int = 1,
) -> List[pathlib.Path]:
    """Generate augmented variants for every image in *input_dir*.

    Parameters
    ----------
    workers : int
        Number of threads to use. 1 = sequential (default).

    Returns a list of paths to the newly created files.
    """
    input_dir = pathlib.Path(input_dir)
    output_dir = pathlib.Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    variants = num_variants or cfg.num_variants
    exts = {".png", ".jpg", ".jpeg", ".bmp", ".gif", ".webp"}
    sources = sorted(p for p in input_dir.iterdir() if p.suffix.lower() in exts)
    total = len(sources) * variants

    created: List[pathlib.Path] = []

    if workers <= 1:
        # Sequential with simple tqdm
        with tqdm(total=total, desc="Augmenting", unit="img") as pbar:
            for src_path in sources:
                paths = _process_single_source(src_path, output_dir, cfg, variants)
                created.extend(paths)
                pbar.update(variants)
    else:
        # Multithreaded
        with ThreadPoolExecutor(max_workers=workers) as pool:
            futures = {
                pool.submit(_process_single_source, src, output_dir, cfg, variants): src
                for src in sources
            }
            with tqdm(total=total, desc=f"Augmenting ({workers} workers)", unit="img") as pbar:
                for future in as_completed(futures):
                    paths = future.result()
                    created.extend(paths)
                    pbar.update(variants)

    return created


# ── Internal helpers ─────────────────────────────────────────────────────────

def _hex_to_tuple(hex_str: str) -> Tuple[int, ...]:
    """Convert ``'#rrggbb'`` or ``'#rrggbbaa'`` to an int tuple."""
    h = hex_str.lstrip("#")
    return tuple(int(h[i : i + 2], 16) for i in range(0, len(h), 2))


def _rgb_to_hsv(rgb: np.ndarray) -> np.ndarray:
    """Vectorised RGB→HSV.  Input/output in [0, 1] range, shape (H, W, 3)."""
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    maxc = np.max(rgb, axis=-1)
    minc = np.min(rgb, axis=-1)
    diff = maxc - minc

    h = np.zeros_like(maxc)
    s = np.zeros_like(maxc)
    v = maxc

    # Saturation
    mask = maxc > 0
    s[mask] = diff[mask] / maxc[mask]

    # Hue
    mask_r = (maxc == r) & (diff > 0)
    mask_g = (maxc == g) & (diff > 0)
    mask_b = (maxc == b) & (diff > 0)
    h[mask_r] = ((g[mask_r] - b[mask_r]) / diff[mask_r]) % 6
    h[mask_g] = ((b[mask_g] - r[mask_g]) / diff[mask_g]) + 2
    h[mask_b] = ((r[mask_b] - g[mask_b]) / diff[mask_b]) + 4
    h /= 6.0

    return np.stack([h, s, v], axis=-1)


def _hsv_to_rgb(hsv: np.ndarray) -> np.ndarray:
    """Vectorised HSV→RGB.  Input/output in [0, 1] range, shape (H, W, 3)."""
    h, s, v = hsv[..., 0], hsv[..., 1], hsv[..., 2]
    i = (h * 6.0).astype(np.int32) % 6
    f = (h * 6.0) - np.floor(h * 6.0)
    p = v * (1.0 - s)
    q = v * (1.0 - s * f)
    t = v * (1.0 - s * (1.0 - f))

    rgb = np.zeros_like(hsv)
    for idx, (r, g, b) in enumerate(
        [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)]
    ):
        mask = i == idx
        rgb[mask, 0] = r[mask]
        rgb[mask, 1] = g[mask]
        rgb[mask, 2] = b[mask]

    return rgb
