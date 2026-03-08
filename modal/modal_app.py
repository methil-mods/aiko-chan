import modal
import subprocess
import os

app = modal.App("aiko-backend-llama-cpp")

# Define the image with CUDA runtime + pre-built llama-cpp-python wheels
# Uses runtime (not devel) for smaller image = faster cold start
# Pre-built CUDA wheels = no compilation needed
image = (
    modal.Image.from_registry(
        "nvidia/cuda:12.4.1-devel-ubuntu22.04",
        add_python="3.11",
    )
    .entrypoint([])
    .pip_install(
        "llama-cpp-python[server]",
        extra_index_url="https://abetlen.github.io/llama-cpp-python/whl/cu124",
    )
)

volume = modal.Volume.from_name("aiko-models", create_if_missing=True)

MODEL_PATH = "/models/aiko-q4.gguf"
MINUTES = 60

@app.function(
    image=image,
    gpu="L40S",
    scaledown_window=3 * MINUTES,
    timeout=1 * MINUTES,
    volumes={"/models": volume},
    secrets=[modal.Secret.from_name("AIKO")]
)
@modal.web_server(port=8000, startup_timeout=120)
def serve():
    # Verify model file exists
    if not os.path.exists(MODEL_PATH):
        models_dir = os.listdir("/models") if os.path.exists("/models") else "Directory not found"
        raise FileNotFoundError(f"Model file not found at {MODEL_PATH}. Contents of /models: {models_dir}")

    # Build command for llama-cpp-python OpenAI-compatible server
    cmd = [
        "python3", "-m", "llama_cpp.server",
        "--model", MODEL_PATH,
        "--host", "0.0.0.0",
        "--port", "8000",
        "--n_gpu_layers", "-1",  # Offload all layers to GPU
        "--chat_format", "chatml",
    ]

    # Check if an API_KEY environment variable is set
    api_key = os.environ.get("AIKO_API_KEY")
    if api_key:
        cmd.extend(["--api_key", api_key])

    print(f"Starting llama-cpp server with command: {' '.join(cmd)}")
    subprocess.Popen(cmd)

@app.local_entrypoint()
def main():
    print("Note: To interact with this server locally, it must be running via `modal serve modal/modal_app.py`")
    print("Or deployed via `modal deploy modal/modal_app.py`")
    print("If deployed, check the Modal dashboard for the endpoint URL.")

    print("\nTip: Make sure the model is uploaded to the 'aiko-models' volume.")
    print("Run: modal volume put aiko-models models/aiko-q4.gguf /aiko-q4.gguf")
