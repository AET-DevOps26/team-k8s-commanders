# AI Assistant Service

FastAPI service for the `/ai/query` endpoint. It builds lightweight patient and appointment context, sends the prompt to the configured LLM provider, and returns an OpenAPI-shaped response.

## Project Structure

```text
services/ai-assistant/
├── main.py              # FastAPI app entry point
├── routes/
│   ├── health.py        # Health endpoint
│   └── query.py         # /ai/query endpoint
├── models/              # Generated request/response models
├── utils/
│   ├── llm.py           # LLM provider selection and clients
│   ├── mock_data.py     # Sample patient/appointment data
│   └── prompt_templates.py
├── tests/               # Unit tests
├── Dockerfile
├── requirements.txt
└── .env.example
```

## Configuration

Create `services/ai-assistant/.env` from `.env.example` if you want to override the defaults.

Useful variables:

- `LLM_PROVIDER=openwebui` or `openai`
- `OPENWEBUI_BASE_URL` for OpenWebUI
- `LLM_API_KEY` for API access
- `LLM_MODEL` for the model name

## Run Locally

```bash
cd services/ai-assistant
python main.py
```

Or with Docker:

```bash
docker compose up
```

## Endpoints

- `GET /ai/health`
- `POST /ai/query`

Example request:

```bash
curl -X POST http://localhost:8000/ai/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Give me a short summary of this patient.",
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
    "appointmentId": "660e8400-e29b-41d4-a716-446655440000"
  }'
```

## Testing

```bash
cd services/ai-assistant
./run_tests.sh
```
