# dataset-aiko

Pixel art dataset augmentation & filtering toolkit for diffusion model fine-tuning.

## Features

- **Pixel-safe augmentations** — scale, hue shift, saturation, brightness, translation, horizontal flip, and palette swap — all using nearest-neighbor interpolation to preserve hard pixel edges.
- **Expression / pose filtering** — label a small seed set, then auto-classify the rest using CNN embeddings + SVM/KNN.
- **Dataset utilities** — scan, deduplicate (perceptual or exact), export metadata (JSON/CSV), and train/val split.
- **YAML-driven config** — all parameters in a single config file.

## Quickstart

```bash
# 1. Install
pip install -e ".[dev]"

# 2. Put your pixel art sprites in data/raw/

# 3. Copy the example config
cp config.example.yaml config.yaml

# 4. Augment
python -m src augment -c config.yaml -w 8

# -w is the amount of worker / thread to work on the augmentation in parelle

# 5. Label a seed set for filtering
python -m src label -c config.yaml

# 6. Auto-filter by expression / pose
python -m src train-filter -c config.yaml

# 7. View stats
python -m src stats -i data/raw
python -m src stats -i data/augmented
```

## CLI Commands

| Command | Description |
|---------|-------------|
| `augment` | Generate augmented variants for every image in the raw directory |
| `label` | Interactively label images for the filter classifier |
| `train-filter` | Train classifier on seed labels and auto-sort images |
| `filter` | Alias for `train-filter` |
| `stats` | Print dataset statistics (count, sizes, formats) |
| `dedupe` | Find and optionally remove duplicate images |
| `export` | Export dataset metadata to JSON or CSV |

## Configuration

See [`config.example.yaml`](config.example.yaml) for all available options.

### Augmentation Options

| Option | Type | Description |
|--------|------|-------------|
| `num_variants` | int | Number of augmented images per source |
| `scale` | list[int] | Integer scale factors (nearest-neighbor) |
| `hue_shift` | [min, max] | Hue rotation in degrees |
| `saturation` | [min, max] | Saturation multiplier range |
| `value` | [min, max] | Brightness multiplier range |
| `translate_x/y` | [min, max] | Pixel translation range |
| `flip_horizontal` | bool | Enable horizontal flip |
| `bg_color` | [R,G,B,A] | Background fill for translation |
| `palette_swap` | dict | Source→target hex color mapping |

### Filter Options

| Option | Type | Description |
|--------|------|-------------|
| `model` | str | Backbone CNN (`resnet18`, `resnet34`, `resnet50`) |
| `categories` | list | Expression/pose category names |
| `seed_labels_path` | str | Path to seed labels JSON |
| `classifier` | str | `svm` or `knn` |

## Project Structure

```
dataset_aiko/
├── pyproject.toml
├── requirements.txt
├── config.example.yaml
├── data/
│   ├── raw/            # Place your source images here
│   ├── augmented/      # Augmented output
│   └── filtered/       # Filtered by category
├── src/
│   ├── __init__.py
│   ├── __main__.py
│   ├── cli.py
│   ├── config.py
│   ├── augmentor.py
│   ├── filters.py
│   └── dataset.py
└── tests/
```
