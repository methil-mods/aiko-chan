import modal
import subprocess
import os

app = modal.App("aiko-backend-llama-cpp")

# Pre-built CUDA wheels = no compilation, fast image build
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
    gpu="T4",              # T4 = faster to allocate, plenty for 4B Q4
    scaledown_window=3 * MINUTES,
    timeout=1 * MINUTES,
    volumes={"/models": volume},
    secrets=[modal.Secret.from_name("AIKO")],
)
@modal.web_server(port=8000, startup_timeout=120)
def serve():
    if not os.path.exists(MODEL_PATH):
        models_dir = os.listdir("/models") if os.path.exists("/models") else "N/A"
        raise FileNotFoundError(f"Model not found at {MODEL_PATH}. /models: {models_dir}")

    cmd = [
        "python3", "-m", "llama_cpp.server",
        "--model", MODEL_PATH,
        "--host", "0.0.0.0",
        "--port", "8000",
        "--n_gpu_layers", "-1",
        "--chat_format", "chatml",
        "--n_ctx", "2048",
    ]

    api_key = os.environ.get("AIKO_API_KEY")
    if api_key:
        cmd.extend(["--api_key", api_key])

    print(f"Starting llama-cpp server: {' '.join(cmd)}")
    subprocess.Popen(cmd)

@app.local_entrypoint()
def main():
    print("Deploy: modal deploy modal/modal_app.py")
    print("Upload model: modal volume put aiko-models models/aiko-q4.gguf /aiko-q4.gguf")