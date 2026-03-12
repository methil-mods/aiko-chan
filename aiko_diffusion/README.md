# aiko_diffusion

Diffusion model fine-tuning and dataset toolkit for pixel art.

## Structure

- `/dataset`: Toolkit for pixel art augmentation and filtering.
- `train_lora.py`: Specialized training script for Flux LoRA fine-tuning.
- `lora_config.yaml`: Configuration for LoRA training (optimized for weird pixel art).

## Quickstart: Fine-tuning for Weird Pixel Art

### 1. Prepare Dataset
Use the `dataset` toolkit to augment your "weird" pixel art images.
```bash
cd dataset
# Put raw images in data/raw/
python -m src augment -c config.yaml
```

### 2. Install Dependencies
It is recommended to use a virtual environment or Conda, then install the required packages.
```bash
pip install -r requirements.txt
```

### 3. Configure Training
Edit `lora_config.yaml` to adjust hyperparameters:
- **QLoRA (8GB VRAM)**: Set `quantization.enabled: true` and `bits: 4`.
- `rank`: Increase (e.g., 16 or 32) for more "weird" detail.
- `trigger_word`: The word used to invoke your style (default: "weird pixel art style").

### 4. Run Training
```bash
python train_lora.py --config lora_config.yaml
```

### 5. Use your LoRA
Once training is finished, your LoRA will be saved in `output/weird_pixel_art/final_lora`. You can then load it into ComfyUI or a custom Flux pipeline.

```python
from diffusers import FluxPipeline
pipe = FluxPipeline.from_pretrained("black-forest-labs/FLUX.1-schnell")
pipe.load_lora_weights("output/weird_pixel_art/final_lora")
```

## "Weird" Pixel Art & VRAM Tips
- **QLoRA (8GB VRAM)**: The trainer supports 4-bit quantization via `bitsandbytes`. Ensure `quantization.enabled` is `true` in your config to fit Flux on a mid-range GPU.
- **Nearest-Neighbor**: The trainer automatically uses nearest-neighbor upscaling to prevent blurring of sharp pixel edges.
- **High Rank**: LoRAs for abstract or weird styles often benefit from a higher rank (`rank: 16`) to learn complex deformations.
- **Trigger Words**: Use unique words in your prompts to separate the style from standard pixel art.
