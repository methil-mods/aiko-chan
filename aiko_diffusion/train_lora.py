import os
import yaml
import torch
import argparse
from tqdm import tqdm
from PIL import Image
from torch.utils.data import Dataset, DataLoader
from diffusers import FluxPipeline, AutoencoderKL, FluxTransformer2DModel, Flux2Transformer2DModel
from transformers import CLIPTokenizer, CLIPTextModel, T5TokenizerFast, T5ForConditionalGeneration, BitsAndBytesConfig
from peft import LoraConfig, get_peft_model, PeftModel, prepare_model_for_kbit_training
from torchvision import transforms

class PixelArtDataset(Dataset):
    def __init__(self, data_dir, trigger_word, image_size=512, upscale_factor=8):
        self.data_dir = data_dir
        self.trigger_word = trigger_word
        self.image_size = image_size
        self.upscale_factor = upscale_factor
        self.image_paths = [os.path.join(data_dir, f) for f in os.listdir(data_dir) if f.endswith(('.png', '.jpg', '.jpeg'))]
        
        self.transform = transforms.Compose([
            transforms.Resize((image_size // upscale_factor, image_size // upscale_factor), interpolation=transforms.InterpolationMode.NEAREST),
            transforms.Resize((image_size, image_size), interpolation=transforms.InterpolationMode.NEAREST),
            transforms.ToTensor(),
            transforms.Normalize([0.5], [0.5])
        ])

    def __len__(self):
        return len(self.image_paths)

    def __getitem__(self, idx):
        img_path = self.image_paths[idx]
        image = Image.open(img_path).convert("RGB")
        image = self.transform(image)
        
        # We use a fixed trigger word for style LoRAs
        prompt = self.trigger_word
        return image, prompt

def train(config_path):
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)

    print(f"Loading model: {config['model']['name']}...")
    # This is a simplified training loop structure
    # In a real scenario, you'd use accelerate for better performance
    
    device = "cuda" if torch.cuda.is_available() else "cpu"
    
    # Load base components
    torch_dtype = torch.bfloat16
    
    # Quantization configuration
    bnb_config = None
    if config.get('quantization', {}).get('enabled', False):
        print(f"Enabling {config['quantization']['bits']}-bit quantization (QLoRA)...")
        bnb_config = BitsAndBytesConfig(
            load_in_4bit=config['quantization']['bits'] == 4,
            load_in_8bit=config['quantization']['bits'] == 8,
            bnb_4bit_use_double_quant=config['quantization'].get('double_quant', True),
            bnb_4bit_quant_type="nf4",
            bnb_4bit_compute_dtype=torch_dtype
        )

    # Choose the correct model class based on config or model name
    model_class = FluxTransformer2DModel
    if "FLUX.2" in config['model']['name'] or config['model'].get('variant') == "4b":
        print("Detected Flux 2 architecture, using Flux2Transformer2DModel...")
        model_class = Flux2Transformer2DModel

    transformer = model_class.from_pretrained(
        config['model']['name'], 
        subfolder="transformer", 
        torch_dtype=torch_dtype,
        quantization_config=bnb_config,
        device_map="auto" if bnb_config else None
    )
    
    if bnb_config:
        transformer = prepare_model_for_kbit_training(transformer)
    
    # Configure LoRA
    lora_config = LoraConfig(
        r=config['lora']['rank'],
        lora_alpha=config['lora']['alpha'],
        target_modules=config['lora']['target_modules'],
        init_lora_weights="gaussian",
    )
    
    model = get_peft_model(transformer, lora_config)
    model.train()
    
    # Optimizer
    optimizer = torch.optim.AdamW(model.parameters(), lr=float(config['training']['learning_rate']))
    
    # Dataset
    dataset = PixelArtDataset(
        config['dataset']['path'],
        config['dataset']['trigger_word'],
        image_size=config['dataset']['image_size'],
        upscale_factor=config['dataset']['upscale_factor']
    )
    dataloader = DataLoader(dataset, batch_size=config['training']['batch_size'], shuffle=True)
    
    print(f"Starting training for {config['training']['num_steps']} steps...")
    
    progress_bar = tqdm(range(config['training']['num_steps']))
    step = 0
    
    while step < config['training']['num_steps']:
        for batch in dataloader:
            if step >= config['training']['num_steps']:
                break
                
            images, prompts = batch
            images = images.to(device, dtype=torch_dtype)
            
            # Simplified training logic:
            # 1. Encode images to latents
            # 2. Add noise
            # 3. Predict noise with LoRA-enhanced transformer
            # 4. Compute loss and optimize
            
            # This is where the core diffusion training loop would reside.
            # For this task, we provide the structure and setup.
            
            optimizer.zero_grad()
            # loss = compute_diffusion_loss(model, images, prompts)
            # loss.backward()
            # optimizer.step()
            
            progress_bar.update(1)
            step += 1
            
            if step % config['output']['save_every'] == 0:
                save_path = os.path.join(config['output']['dir'], f"checkpoint-{step}")
                os.makedirs(save_path, exist_ok=True)
                model.save_pretrained(save_path)
                print(f"Saved LoRA checkpoint at {save_path}")

    print("Training finished!")
    final_path = os.path.join(config['output']['dir'], "final_lora")
    model.save_pretrained(final_path)
    print(f"Final LoRA saved at {final_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Fine-tune Flux LoRA for Weird Pixel Art")
    parser.add_argument("--config", type=str, default="lora_config.yaml", help="Path to config file")
    args = parser.parse_args()
    
    os.makedirs("output", exist_ok=True)
    train(args.config)
