"""
test_config.py — Unit tests for YAML config loading.
"""

import pathlib
import textwrap

from src.config import AugmentationConfig, Config, FilterConfig, PathsConfig, load_config


class TestLoadConfig:
    def test_loads_example(self, tmp_path):
        cfg_text = textwrap.dedent("""\
            paths:
              raw: "data/raw"
              augmented: "data/augmented"
              filtered: "data/filtered"

            augmentation:
              num_variants: 5
              scale: [1, 2]
              hue_shift: [-15, 15]
              saturation: [0.9, 1.1]
              value: [0.9, 1.1]
              translate_x: [-4, 4]
              translate_y: [-4, 4]
              flip_horizontal: true
              bg_color: [0, 0, 0, 0]

            filter:
              model: "resnet18"
              categories: ["happy", "sad"]
              classifier: "knn"
        """)
        cfg_file = tmp_path / "config.yaml"
        cfg_file.write_text(cfg_text, encoding="utf-8")

        cfg = load_config(cfg_file)
        assert isinstance(cfg, Config)
        assert cfg.augmentation.num_variants == 5
        assert cfg.augmentation.scale == [1, 2]
        assert cfg.augmentation.hue_shift == (-15.0, 15.0)
        assert cfg.filter.categories == ["happy", "sad"]
        assert cfg.filter.classifier == "knn"
        assert cfg.paths.raw == "data/raw"

    def test_defaults(self, tmp_path):
        cfg_file = tmp_path / "empty.yaml"
        cfg_file.write_text("{}", encoding="utf-8")

        cfg = load_config(cfg_file)
        assert cfg.augmentation.num_variants == 10
        assert cfg.paths.raw == "data/raw"

    def test_missing_file(self):
        import pytest
        with pytest.raises(FileNotFoundError):
            load_config("nonexistent.yaml")
