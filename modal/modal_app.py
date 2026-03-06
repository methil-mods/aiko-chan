import modal
import subprocess
import os
app = modal.App("aiko-backend-vllm")

# Define the image with vLLM
image = (
    modal.Image.debian_slim(python_version="3.11")
    .pip_install(
        "vllm",
        "fastapi[standard]",
        "requests"
    )
    .env({"HF_XET_HIGH_PERFORMANCE": "1"})
)

volume = modal.Volume.from_name("aiko-models", create_if_missing=True)
vllm_cache_vol = modal.Volume.from_name("vllm-cache", create_if_missing=True)

MODEL_PATH = "/models/aiko-q4.gguf"
MINUTES = 60

@app.function(
    image=image,
    gpu="L40S",
    scaledown_window=15 * MINUTES,
    timeout=10 * MINUTES,
    volumes={
        "/models": volume,
        "/root/.cache/vllm": vllm_cache_vol,
        },
    secrets=[modal.Secret.from_name("AIKO")]
)
@modal.web_server(port=8000, startup_timeout=300)
def serve():
    # Start the vLLM OpenAI-compatible server
    cmd = [
        "vllm", "serve", MODEL_PATH,
        "--host", "0.0.0.0",
        "--port", "8000",
        "--max-model-len", "2048",
        "--served-model-name", "aiko",
        "--tokenizer", "Qwen/Qwen3-4B", 
        "--no-enforce-eager"
    ]
    
    # Check if an API_KEY environment variable is set in the Modal app
    api_key = os.environ.get("AIKO_API_KEY")
    if api_key:
        cmd.extend(["--api-key", api_key])
        
    print(f"Starting vLLM server with command: {' '.join(cmd)}")
    subprocess.Popen(cmd)

@app.local_entrypoint()
def main():
    import requests
    import time
    
    print("Note: To interact with this server locally, it must be running via `modal serve backend/modal_app.py`")
    print("Or deployed via `modal deploy backend/modal_app.py`")
    print("If deployed, check the Modal dashboard for the endpoint URL.")
    
    print("\nTip: Make sure the model is uploaded to the 'aiko-models' volume.")
    print("Run: modal volume put aiko-models models/aiko-q4.gguf /aiko-q4.gguf")
