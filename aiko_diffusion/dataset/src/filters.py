"""
filters.py — Expression & body-pose classifier for pixel art sprites.

Strategy:
1. Use a pretrained CNN (ResNet-18 by default) as a frozen feature extractor.
2. The user labels a small seed set (~5-10 images per category) via the
   interactive labeler.
3. A lightweight SVM / KNN is trained on the resulting embeddings.
4. The trained classifier auto-labels the remaining images and sorts
   them into per-category folders.
"""

from __future__ import annotations

import json
import pathlib
import sys
from typing import Dict, List, Optional, Tuple

import numpy as np
from PIL import Image
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import SVC

from .config import FilterConfig

# Lazy-import torch so the rest of the package stays usable without it.
_torch = None
_torchvision = None


def _ensure_torch():
    global _torch, _torchvision
    if _torch is None:
        import torch
        import torchvision
        _torch = torch
        _torchvision = torchvision


# ── Feature extraction ───────────────────────────────────────────────────────

def _get_model(name: str = "resnet18"):
    """Return a frozen pretrained model without the final FC layer."""
    _ensure_torch()
    import torchvision.models as models
    import torch.nn as nn

    weights_map = {
        "resnet18": (models.resnet18, models.ResNet18_Weights.DEFAULT),
        "resnet34": (models.resnet34, models.ResNet34_Weights.DEFAULT),
        "resnet50": (models.resnet50, models.ResNet50_Weights.DEFAULT),
    }
    if name not in weights_map:
        raise ValueError(f"Unsupported model: {name!r}. Choose from {list(weights_map)}")

    factory, weights = weights_map[name]
    model = factory(weights=weights)
    # Remove the final classification head — we only want embeddings.
    model = nn.Sequential(*list(model.children())[:-1])
    model.eval()
    for p in model.parameters():
        p.requires_grad_(False)
    return model, weights.transforms()


def extract_embeddings(
    image_paths: List[pathlib.Path],
    model_name: str = "resnet18",
    batch_size: int = 16,
) -> Tuple[np.ndarray, list]:
    """Extract feature embeddings for a list of images.

    Returns
    -------
    embeddings : np.ndarray, shape (N, D)
    filenames  : list of str (basenames, for mapping back to files)
    """
    _ensure_torch()
    import torch

    model, transform = _get_model(model_name)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = model.to(device)

    embeddings: List[np.ndarray] = []
    filenames: List[str] = []

    for start in range(0, len(image_paths), batch_size):
        batch_paths = image_paths[start : start + batch_size]
        tensors = []
        for p in batch_paths:
            img = Image.open(p).convert("RGB")
            tensors.append(transform(img))
            filenames.append(p.name)
        batch = torch.stack(tensors).to(device)

        with torch.no_grad():
            feats = model(batch)
        feats = feats.squeeze(-1).squeeze(-1).cpu().numpy()
        embeddings.append(feats)

    return np.concatenate(embeddings, axis=0), filenames


# ── Classifier ───────────────────────────────────────────────────────────────

def train_classifier(
    embeddings: np.ndarray,
    labels: List[str],
    kind: str = "svm",
):
    """Train a lightweight classifier on labeled embeddings.

    Parameters
    ----------
    kind : 'svm' or 'knn'
    """
    if kind == "svm":
        clf = SVC(kernel="rbf", probability=True)
    elif kind == "knn":
        n_neighbors = min(5, len(labels))
        clf = KNeighborsClassifier(n_neighbors=n_neighbors)
    else:
        raise ValueError(f"Unknown classifier kind: {kind!r}")

    clf.fit(embeddings, labels)
    return clf


def predict(
    embeddings: np.ndarray,
    classifier,
) -> List[str]:
    """Return predicted category labels for *embeddings*."""
    return classifier.predict(embeddings).tolist()


# ── Dataset filtering ────────────────────────────────────────────────────────

def filter_dataset(
    input_dir: str | pathlib.Path,
    output_dir: str | pathlib.Path,
    cfg: FilterConfig,
) -> Dict[str, List[pathlib.Path]]:
    """Auto-classify images and sort them into per-category sub-folders.

    Requires a seed labels JSON file (``{ "filename": "category" }``).

    Returns a dict mapping category → list of output paths.
    """
    input_dir = pathlib.Path(input_dir)
    output_dir = pathlib.Path(output_dir)

    # Load seed labels
    seed_path = pathlib.Path(cfg.seed_labels_path)
    if not seed_path.exists():
        raise FileNotFoundError(
            f"Seed labels file not found: {seed_path}\n"
            "Run the 'label' command first to create it."
        )
    with open(seed_path, "r", encoding="utf-8") as fh:
        seed_labels: Dict[str, str] = json.load(fh)

    # Gather all images
    exts = {".png", ".jpg", ".jpeg", ".bmp", ".gif", ".webp"}
    all_images = sorted(p for p in input_dir.iterdir() if p.suffix.lower() in exts)
    if not all_images:
        raise RuntimeError(f"No images found in {input_dir}")

    # Extract embeddings for everything
    embeddings, filenames = extract_embeddings(all_images, model_name=cfg.model)

    # Split into labeled (seed) and unlabeled
    labeled_idx = [i for i, fn in enumerate(filenames) if fn in seed_labels]
    unlabeled_idx = [i for i, fn in enumerate(filenames) if fn not in seed_labels]

    if not labeled_idx:
        raise RuntimeError("No seed labels match any image filenames in the input directory.")

    X_train = embeddings[labeled_idx]
    y_train = [seed_labels[filenames[i]] for i in labeled_idx]

    # Train & predict
    clf = train_classifier(X_train, y_train, kind=cfg.classifier)

    if unlabeled_idx:
        X_test = embeddings[unlabeled_idx]
        preds = predict(X_test, clf)
    else:
        preds = []

    # Build full label map
    label_map: Dict[str, str] = {}
    for i in labeled_idx:
        label_map[filenames[i]] = seed_labels[filenames[i]]
    for idx, pred in zip(unlabeled_idx, preds):
        label_map[filenames[idx]] = pred

    # Copy/sort into category sub-directories
    result: Dict[str, List[pathlib.Path]] = {}
    for img_path in all_images:
        cat = label_map.get(img_path.name, "unknown")
        cat_dir = output_dir / cat
        cat_dir.mkdir(parents=True, exist_ok=True)
        dst = cat_dir / img_path.name
        # Use Pillow to re-save (avoids shutil import just for copy)
        Image.open(img_path).save(dst)
        result.setdefault(cat, []).append(dst)

    return result


# ── Interactive labeler ──────────────────────────────────────────────────────

def interactive_labeler(
    input_dir: str | pathlib.Path,
    categories: List[str],
    output_path: str | pathlib.Path = "data/seed_labels.json",
    existing: Optional[Dict[str, str]] = None,
) -> Dict[str, str]:
    """CLI-based interactive labeler.

    Displays each filename and asks the user to choose a category.
    Labels are saved to *output_path* as JSON.
    """
    input_dir = pathlib.Path(input_dir)
    output_path = pathlib.Path(output_path)

    exts = {".png", ".jpg", ".jpeg", ".bmp", ".gif", ".webp"}
    images = sorted(p for p in input_dir.iterdir() if p.suffix.lower() in exts)

    labels: Dict[str, str] = dict(existing) if existing else {}

    print(f"\n{'─' * 50}")
    print(f" Interactive Labeler — {len(images)} images")
    print(f" Categories: {', '.join(categories)}")
    print(f" Type the category number, 's' to skip, 'q' to quit.")
    print(f"{'─' * 50}\n")

    cat_menu = "\n".join(f"  [{i}] {c}" for i, c in enumerate(categories))

    for img_path in images:
        if img_path.name in labels:
            print(f"  (already labeled) {img_path.name} → {labels[img_path.name]}")
            continue

        print(f"\n  Image: {img_path.name}")
        print(cat_menu)
        choice = input("  > ").strip().lower()

        if choice == "q":
            break
        if choice == "s":
            continue
        try:
            idx = int(choice)
            if 0 <= idx < len(categories):
                labels[img_path.name] = categories[idx]
                print(f"    → {categories[idx]}")
            else:
                print("    (invalid index, skipping)")
        except ValueError:
            # Allow typing the category name directly
            if choice in categories:
                labels[img_path.name] = choice
                print(f"    → {choice}")
            else:
                print("    (unrecognised, skipping)")

    # Save
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as fh:
        json.dump(labels, fh, indent=2)
    print(f"\n  ✓ Saved {len(labels)} labels → {output_path}")

    return labels
