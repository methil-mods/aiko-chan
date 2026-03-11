"""
test_dataset.py — Unit tests for dataset scanning, deduplication, and export.
"""

import json
import pathlib
import tempfile

import numpy as np
import pytest
from PIL import Image

from src.dataset import (
    ImageMeta,
    deduplicate,
    export_metadata,
    scan_directory,
    split_dataset,
)


# ── Helpers ──────────────────────────────────────────────────────────────────

def _create_temp_images(tmp_dir: pathlib.Path, n: int = 3) -> list[pathlib.Path]:
    """Create *n* small PNG images in *tmp_dir* with distinct pixels."""
    paths = []
    for i in range(n):
        arr = np.full((4, 4, 4), fill_value=(i * 50) % 256, dtype=np.uint8)
        arr[:, :, 3] = 255
        img = Image.fromarray(arr, "RGBA")
        p = tmp_dir / f"img_{i:03d}.png"
        img.save(p)
        paths.append(p)
    return paths


# ── Tests ────────────────────────────────────────────────────────────────────

class TestScanDirectory:
    def test_finds_images(self, tmp_path):
        _create_temp_images(tmp_path, 3)
        results = scan_directory(tmp_path)
        assert len(results) == 3
        assert all(isinstance(m, ImageMeta) for m in results)

    def test_empty_dir(self, tmp_path):
        results = scan_directory(tmp_path)
        assert results == []

    def test_ignores_non_images(self, tmp_path):
        (tmp_path / "readme.txt").write_text("hello")
        _create_temp_images(tmp_path, 1)
        results = scan_directory(tmp_path)
        assert len(results) == 1


class TestDeduplicate:
    def test_exact_dedup(self, tmp_path):
        # Create two identical images (all-zeros RGBA)
        arr = np.zeros((4, 4, 4), dtype=np.uint8)
        arr[:, :, 3] = 255
        for name in ("a.png", "b.png"):
            Image.fromarray(arr, "RGBA").save(tmp_path / name)
        # Create one distinct image (different pixel values)
        unique_arr = np.full((4, 4, 4), fill_value=128, dtype=np.uint8)
        unique_arr[:, :, 3] = 255
        Image.fromarray(unique_arr, "RGBA").save(tmp_path / "c.png")

        images = scan_directory(tmp_path)
        unique, dups = deduplicate(images, by="exact")
        assert len(dups) == 1  # one of the identical pair

    def test_no_dups(self, tmp_path):
        _create_temp_images(tmp_path, 3)
        images = scan_directory(tmp_path)
        unique, dups = deduplicate(images, by="exact")
        assert len(dups) == 0
        assert len(unique) == 3


class TestExportMetadata:
    def test_json_export(self, tmp_path):
        _create_temp_images(tmp_path, 2)
        images = scan_directory(tmp_path)
        out = export_metadata(images, tmp_path / "meta.json", fmt="json")
        data = json.loads(out.read_text())
        assert len(data) == 2

    def test_csv_export(self, tmp_path):
        _create_temp_images(tmp_path, 2)
        images = scan_directory(tmp_path)
        out = export_metadata(images, tmp_path / "meta.csv", fmt="csv")
        lines = out.read_text().strip().split("\n")
        assert len(lines) == 3  # header + 2 rows


class TestSplitDataset:
    def test_split_ratios(self, tmp_path):
        _create_temp_images(tmp_path, 10)
        images = scan_directory(tmp_path)
        splits = split_dataset(images, train_ratio=0.8)
        assert len(splits["train"]) == 8
        assert len(splits["val"]) == 2

    def test_deterministic(self, tmp_path):
        _create_temp_images(tmp_path, 10)
        images = scan_directory(tmp_path)
        s1 = split_dataset(images, seed=0)
        s2 = split_dataset(images, seed=0)
        assert [m.filename for m in s1["train"]] == [m.filename for m in s2["train"]]
