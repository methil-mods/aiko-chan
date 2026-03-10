import modal
import subprocess
import os
import time
import requests

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
        "requests",
        extra_index_url="https://abetlen.github.io/llama-cpp-python/whl/cu124",
    )
)

volume = modal.Volume.from_name("aiko-models", create_if_missing=True)

MODEL_PATH = "/models/aiko-q4.gguf"
MINUTES = 60

@app.cls(
    image=image,
    gpu="T4",              # T4 = faster to allocate, plenty for 4B Q4
    scaledown_window=3 * MINUTES,
    timeout=10 * MINUTES,
    volumes={"/models": volume},
    secrets=[modal.Secret.from_name("AIKO")],
    enable_memory_snapshot=True,
    experimental_options={"enable_gpu_snapshot": True},
)
class LlamaServer:
    @modal.enter(snap=True)
    def start_server(self):
        if not os.path.exists(MODEL_PATH):
            raise FileNotFoundError(f"Model not found at {MODEL_PATH}")

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

        print(f"Starting llama-cpp server for snapshot: {' '.join(cmd)}")
        self.process = subprocess.Popen(cmd)

        # Wait for the server to be ready before taking the snapshot
        max_retries = 30
        for i in range(max_retries):
            try:
                response = requests.get("http://localhost:8000/health")
                if response.status_code == 200:
                    print("Server is ready for snapshot!")
                    return
            except Exception:
                pass
            time.sleep(2)
        
        raise TimeoutError("Llama-cpp server failed to start within timeout")

    @modal.web_server(port=8000)
    def serve(self):
        # The server is already started by @modal.enter
        pass

@app.local_entrypoint()
def main():
    print("Deploy: modal deploy modal/modal_app.py")
    print("Upload model: modal volume put aiko-models models/aiko-q4.gguf /aiko-q4.gguf")
