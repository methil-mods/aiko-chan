"""
config.py — Load and validate YAML configuration.
"""

from __future__ import annotations

import pathlib
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple

import yaml


# ── Dataclasses ──────────────────────────────────────────────────────────────

@dataclass
class PathsConfig:
    raw: str = "data/raw"
    augmented: str = "data/augmented"
    filtered: str = "data/filtered"


@dataclass
class AugmentationConfig:
    num_variants: int = 10
    scale: List[int] = field(default_factory=lambda: [1, 2, 3])
    hue_shift: Tuple[float, float] = (-30.0, 30.0)
    saturation: Tuple[float, float] = (0.7, 1.3)
    value: Tuple[float, float] = (0.8, 1.2)
    translate_x: Tuple[int, int] = (-8, 8)
    translate_y: Tuple[int, int] = (-8, 8)
    flip_horizontal: bool = True
    bg_color: Tuple[int, ...] = (0, 0, 0, 0)
    palette_swap: Optional[Dict[str, str]] = None


@dataclass
class FilterConfig:
    model: str = "resnet18"
    categories: List[str] = field(
        default_factory=lambda: ["happy", "sad", "neutral", "walking", "idle", "attacking"]
    )
    seed_labels_path: str = "data/seed_labels.json"
    classifier: str = "svm"


@dataclass
class Config:
    paths: PathsConfig = field(default_factory=PathsConfig)
    augmentation: AugmentationConfig = field(default_factory=AugmentationConfig)
    filter: FilterConfig = field(default_factory=FilterConfig)


# ── Loader ───────────────────────────────────────────────────────────────────

def _coerce_tuple(val: Any, cast: type = float) -> Tuple:
    """Convert a list from YAML into a tuple with the given element type."""
    if isinstance(val, (list, tuple)):
        return tuple(cast(v) for v in val)
    return val


def load_config(path: str | pathlib.Path) -> Config:
    """Load a YAML config file and return a validated *Config* object."""
    path = pathlib.Path(path)
    if not path.exists():
        raise FileNotFoundError(f"Config file not found: {path}")

    with open(path, "r", encoding="utf-8") as fh:
        raw: Dict[str, Any] = yaml.safe_load(fh) or {}

    # -- paths -----------------------------------------------------------------
    paths_raw = raw.get("paths", {})
    paths = PathsConfig(**{k: v for k, v in paths_raw.items() if k in PathsConfig.__dataclass_fields__})

    # -- augmentation ----------------------------------------------------------
    aug_raw = raw.get("augmentation", {})
    aug_fields: Dict[str, Any] = {}
    for key, val in aug_raw.items():
        if key in ("hue_shift", "saturation", "value"):
            aug_fields[key] = _coerce_tuple(val, float)
        elif key in ("translate_x", "translate_y"):
            aug_fields[key] = _coerce_tuple(val, int)
        elif key == "bg_color":
            aug_fields[key] = _coerce_tuple(val, int)
        elif key == "scale":
            aug_fields[key] = [int(v) for v in val] if isinstance(val, list) else val
        elif key in AugmentationConfig.__dataclass_fields__:
            aug_fields[key] = val
    augmentation = AugmentationConfig(**aug_fields)

    # -- filter ----------------------------------------------------------------
    filt_raw = raw.get("filter", {})
    filt = FilterConfig(**{k: v for k, v in filt_raw.items() if k in FilterConfig.__dataclass_fields__})

    return Config(paths=paths, augmentation=augmentation, filter=filt)
