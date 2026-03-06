# Aiko Modal Backend

This folder contains the Modal backend for running the Aiko GGUF model (`aiko-q4.gguf`).

## Prerequisites

1.  **Modal Account**: Ensure you have a Modal account and the `modal` CLI is configured.
2.  **Model File**: You should have the GGUF file at `backend/models/aiko-q4.gguf`.

## Setup

### 1. Upload the Model to Modal Volume

Modal uses `Volumes` to store large files. Run the following command from the root of the project to upload the model:

```bash
conda activate aikoback
modal volume put aiko-models backend/models/aiko-q4.gguf /aiko-q4.gguf
```

### 2. Run the App Locally (Testing)

You can run a test inference to ensure everything is working correctly:

```bash
modal run backend/modal_app.py
```

### 3. Deploy the App

To deploy the app and expose the web endpoint:

```bash
modal deploy backend/modal_app.py
```

After deployment, Modal will provide a URL for the web endpoint.

## API Usage

The web endpoint now provides an **OpenAI-compatible API**. You can use the standard `openai` python package or `curl`.

### Authentication (API Key)

You can secure your endpoint by setting a Modal secret.
First, create a secret via the Modal CLI or dashboard, for example named `aiko-secret` containing `API_KEY=your_super_secret_key`.
Then, attach it to your `@app.function` in `modal_app.py`:

```python
@app.function(
    ...,
    secrets=[modal.Secret.from_name("aiko-secret")]
)
```

Example with `curl` (assuming you set an API key):

```bash
curl -X POST https://<your-modal-app-url>.modal.run/v1/chat/completions \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer your_super_secret_key" \
     -d '{
       "model": "aiko",
       "messages": [
         {"role": "user", "content": "salut aiko, ça va ?"}
       ],
       "max_tokens": 256
     }'
```

Because it is OpenAI compatible, you can also pass a `system` prompt in the messages list to enforce Aiko's persona:

```json
{"role": "system", "content": "tu es aiko, 22 ans, mélancolique, tu parles SMS..."}
```

## Performance Note

The app is configured to use an **L40S GPU** and **vLLM**, which provides excellent throughput and fast inference for the 4B model.
