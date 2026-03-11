"""
cli.py — Click-based CLI entry point for dataset-aiko.

Usage:
    python -m src.cli <command> [options]
"""

from __future__ import annotations

import pathlib
import sys

import click

from .config import load_config


# ── CLI Group ────────────────────────────────────────────────────────────────

@click.group()
@click.version_option("0.1.0", prog_name="dataset-aiko")
def cli():
    """Pixel art dataset augmentation & filtering toolkit."""


# ── augment ──────────────────────────────────────────────────────────────────

@cli.command()
@click.option(
    "--config", "-c", "config_path",
    default="config.example.yaml",
    help="Path to YAML configuration file.",
)
@click.option(
    "--num-variants", "-n",
    type=int, default=None,
    help="Override number of augmented variants per image.",
)
@click.option(
    "--workers", "-w",
    type=int, default=1,
    help="Number of threads for parallel augmentation (default: 1).",
)
def augment(config_path: str, num_variants: int | None, workers: int):
    """Run the augmentation pipeline on all images in the raw directory."""
    from .augmentor import augment_dataset

    cfg = load_config(config_path)
    input_dir = cfg.paths.raw
    output_dir = cfg.paths.augmented

    click.echo(f"  ▸ Input:   {input_dir}")
    click.echo(f"  ▸ Output:  {output_dir}")
    click.echo(f"  ▸ Variants per image: {num_variants or cfg.augmentation.num_variants}")
    click.echo(f"  ▸ Workers: {workers}")

    created = augment_dataset(input_dir, output_dir, cfg.augmentation, num_variants, workers=workers)
    click.echo(f"\n  ✓ Created {len(created)} augmented images.")


# ── label ────────────────────────────────────────────────────────────────────

@cli.command()
@click.option(
    "--input", "-i", "input_dir",
    default="data/raw",
    help="Directory containing images to label.",
)
@click.option(
    "--config", "-c", "config_path",
    default="config.example.yaml",
    help="Path to YAML configuration file (for category list).",
)
@click.option(
    "--output", "-o", "output_path",
    default=None,
    help="Where to save the seed labels JSON. Defaults to config value.",
)
def label(input_dir: str, config_path: str, output_path: str | None):
    """Interactively label images for the filter classifier."""
    import json
    from .filters import interactive_labeler

    cfg = load_config(config_path)
    out = output_path or cfg.filter.seed_labels_path

    # Load any existing labels
    existing = {}
    out_p = pathlib.Path(out)
    if out_p.exists():
        with open(out_p, "r", encoding="utf-8") as fh:
            existing = json.load(fh)
        click.echo(f"  ▸ Loaded {len(existing)} existing labels from {out}")

    interactive_labeler(input_dir, cfg.filter.categories, out, existing=existing)


# ── train-filter ─────────────────────────────────────────────────────────────

@cli.command("train-filter")
@click.option(
    "--config", "-c", "config_path",
    default="config.example.yaml",
    help="Path to YAML configuration file.",
)
def train_filter(config_path: str):
    """Train the expression/pose classifier on seed labels and auto-filter."""
    from .filters import filter_dataset

    cfg = load_config(config_path)
    click.echo(f"  ▸ Input:      {cfg.paths.raw}")
    click.echo(f"  ▸ Output:     {cfg.paths.filtered}")
    click.echo(f"  ▸ Model:      {cfg.filter.model}")
    click.echo(f"  ▸ Classifier: {cfg.filter.classifier}")
    click.echo(f"  ▸ Categories: {', '.join(cfg.filter.categories)}")

    result = filter_dataset(cfg.paths.raw, cfg.paths.filtered, cfg.filter)
    for cat, paths in result.items():
        click.echo(f"    {cat}: {len(paths)} images")

    total = sum(len(v) for v in result.values())
    click.echo(f"\n  ✓ Filtered {total} images into {len(result)} categories.")


# ── filter (alias that uses pre-trained classifier) ──────────────────────────

@cli.command()
@click.option(
    "--config", "-c", "config_path",
    default="config.example.yaml",
    help="Path to YAML configuration file.",
)
def filter(config_path: str):
    """Alias for train-filter — classify and sort images."""
    from .filters import filter_dataset

    cfg = load_config(config_path)
    result = filter_dataset(cfg.paths.raw, cfg.paths.filtered, cfg.filter)
    for cat, paths in result.items():
        click.echo(f"    {cat}: {len(paths)} images")

    total = sum(len(v) for v in result.values())
    click.echo(f"\n  ✓ Filtered {total} images into {len(result)} categories.")


# ── stats ────────────────────────────────────────────────────────────────────

@cli.command()
@click.option(
    "--input", "-i", "input_dir",
    default="data/raw",
    help="Directory to scan.",
)
def stats(input_dir: str):
    """Print dataset statistics."""
    from .dataset import scan_directory

    images = scan_directory(input_dir)
    if not images:
        click.echo("  No images found.")
        return

    total_bytes = sum(m.size_bytes for m in images)
    widths = [m.width for m in images]
    heights = [m.height for m in images]

    click.echo(f"\n  Dataset Statistics for: {input_dir}")
    click.echo(f"  {'─' * 40}")
    click.echo(f"  Total images : {len(images)}")
    click.echo(f"  Total size   : {total_bytes / 1024:.1f} KB")
    click.echo(f"  Width range  : {min(widths)}–{max(widths)} px")
    click.echo(f"  Height range : {min(heights)}–{max(heights)} px")
    modes = set(m.mode for m in images)
    click.echo(f"  Color modes  : {', '.join(sorted(modes))}")
    formats = set(m.format or '?' for m in images)
    click.echo(f"  Formats      : {', '.join(sorted(formats))}")


# ── dedupe ───────────────────────────────────────────────────────────────────

@cli.command()
@click.option(
    "--input", "-i", "input_dir",
    default="data/raw",
    help="Directory to deduplicate.",
)
@click.option(
    "--method", "-m",
    type=click.Choice(["exact", "perceptual"]),
    default="perceptual",
    help="Deduplication method.",
)
@click.option(
    "--threshold", "-t",
    type=int, default=5,
    help="Hamming distance threshold for perceptual dedup.",
)
@click.option(
    "--dry-run / --no-dry-run",
    default=True,
    help="If set, only print duplicates without deleting.",
)
def dedupe(input_dir: str, method: str, threshold: int, dry_run: bool):
    """Find and optionally remove duplicate images."""
    from .dataset import scan_directory, deduplicate

    images = scan_directory(input_dir)
    unique, dups = deduplicate(images, by=method, threshold=threshold)

    click.echo(f"\n  Scanned: {len(images)} images")
    click.echo(f"  Unique:  {len(unique)}")
    click.echo(f"  Dupes:   {len(dups)}")

    if dups:
        for d in dups:
            click.echo(f"    ✗ {d.filename}  (hash: {d.perceptual_hash})")
        if not dry_run:
            for d in dups:
                pathlib.Path(d.path).unlink()
            click.echo(f"\n  ✓ Deleted {len(dups)} duplicate(s).")
        else:
            click.echo("\n  (dry run — no files deleted. Use --no-dry-run to delete.)")
    else:
        click.echo("  No duplicates found.")


# ── export ───────────────────────────────────────────────────────────────────

@cli.command()
@click.option(
    "--input", "-i", "input_dir",
    default="data/raw",
    help="Directory to export metadata for.",
)
@click.option(
    "--output", "-o", "output_path",
    default="data/metadata.json",
    help="Output file path (.json or .csv).",
)
@click.option(
    "--format", "-f", "fmt",
    type=click.Choice(["json", "csv"]),
    default="json",
    help="Output format.",
)
def export(input_dir: str, output_path: str, fmt: str):
    """Export dataset metadata to JSON or CSV."""
    from .dataset import scan_directory, export_metadata

    images = scan_directory(input_dir)
    out = export_metadata(images, output_path, fmt=fmt)
    click.echo(f"  ✓ Exported metadata for {len(images)} images → {out}")


# ── __main__ support ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    cli()
