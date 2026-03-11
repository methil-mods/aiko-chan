"""
dataset.py — Dataset scanning, deduplication, metadata export, and splitting.
"""

from __future__ import annotations

import csv
import hashlib
import json
import pathlib
import random
from dataclasses import asdict, dataclass
from typing import Dict, List, Optional, Tuple

import imagehash
from PIL import Image


# ── Data models ──────────────────────────────────────────────────────────────

@dataclass
class ImageMeta:
    path: str
    filename: str
    width: int
    height: int
    mode: str
    format: Optional[str]
    file_hash: str        # SHA-256 of file bytes
    perceptual_hash: str  # average-hash for near-duplicate detection
    size_bytes: int


# ── Scanning ─────────────────────────────────────────────────────────────────

_IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".bmp", ".gif", ".webp"}


def scan_directory(path: str | pathlib.Path) -> List[ImageMeta]:
    """Recursively scan *path* for images and collect metadata."""
    root = pathlib.Path(path)
    if not root.is_dir():
        raise NotADirectoryError(f"Not a directory: {root}")

    results: List[ImageMeta] = []
    for p in sorted(root.rglob("*")):
        if p.suffix.lower() not in _IMAGE_EXTS:
            continue
        try:
            img = Image.open(p)
            file_bytes = p.read_bytes()
            meta = ImageMeta(
                path=str(p),
                filename=p.name,
                width=img.width,
                height=img.height,
                mode=img.mode,
                format=img.format,
                file_hash=hashlib.sha256(file_bytes).hexdigest(),
                perceptual_hash=str(imagehash.average_hash(img)),
                size_bytes=len(file_bytes),
            )
            results.append(meta)
        except Exception as exc:
            print(f"  ⚠ skipping {p}: {exc}")

    return results


# ── Deduplication ────────────────────────────────────────────────────────────

def deduplicate(
    images: List[ImageMeta],
    by: str = "perceptual",
    threshold: int = 5,
) -> Tuple[List[ImageMeta], List[ImageMeta]]:
    """Remove near-duplicate images.

    Parameters
    ----------
    by : 'exact' (SHA-256) or 'perceptual' (average-hash Hamming distance).
    threshold : max Hamming distance for perceptual matching (0 = exact).

    Returns
    -------
    (unique, duplicates)
    """
    unique: List[ImageMeta] = []
    duplicates: List[ImageMeta] = []

    if by == "exact":
        seen_hashes: set = set()
        for m in images:
            if m.file_hash in seen_hashes:
                duplicates.append(m)
            else:
                seen_hashes.add(m.file_hash)
                unique.append(m)
    elif by == "perceptual":
        kept_hashes: List[imagehash.ImageHash] = []
        for m in images:
            h = imagehash.hex_to_hash(m.perceptual_hash)
            is_dup = False
            for kh in kept_hashes:
                if abs(h - kh) <= threshold:
                    is_dup = True
                    break
            if is_dup:
                duplicates.append(m)
            else:
                kept_hashes.append(h)
                unique.append(m)
    else:
        raise ValueError(f"Unknown dedup method: {by!r}")

    return unique, duplicates


# ── Metadata export ──────────────────────────────────────────────────────────

def export_metadata(
    images: List[ImageMeta],
    output: str | pathlib.Path,
    fmt: str = "json",
) -> pathlib.Path:
    """Write image metadata to a JSON or CSV file."""
    output = pathlib.Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    records = [asdict(m) for m in images]

    if fmt == "json":
        with open(output, "w", encoding="utf-8") as fh:
            json.dump(records, fh, indent=2)
    elif fmt == "csv":
        if not records:
            output.write_text("")
            return output
        fieldnames = list(records[0].keys())
        with open(output, "w", newline="", encoding="utf-8") as fh:
            writer = csv.DictWriter(fh, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(records)
    else:
        raise ValueError(f"Unknown format: {fmt!r}")

    return output


# ── Train / validation split ────────────────────────────────────────────────

def split_dataset(
    images: List[ImageMeta],
    train_ratio: float = 0.8,
    seed: int = 42,
) -> Dict[str, List[ImageMeta]]:
    """Split images into train and validation sets."""
    rng = random.Random(seed)
    shuffled = list(images)
    rng.shuffle(shuffled)
    cut = int(len(shuffled) * train_ratio)
    return {"train": shuffled[:cut], "val": shuffled[cut:]}
