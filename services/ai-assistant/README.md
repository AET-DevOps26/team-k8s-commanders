# AI Assistant Service

FastAPI service for the AI assistant. It manages persistent conversation **sessions**: each session is owned by a user, optionally bound to a patient/appointment, and stores the full message history. Every message turn re-fetches live patient/appointment context and replays the prior conversation to the configured LLM, returning an OpenAPI-shaped response (with optional Server-Sent Events streaming).

Sessions are stored in a Postgres database (`ai-db`); the schema is created on startup for dev (production should use migrations).

## Project Structure

```text
services/ai-assistant/
├── main.py              # FastAPI app entry point (creates tables on startup)
├── routes/
│   ├── health.py        # Health endpoint
│   └── sessions.py      # /ai/sessions* endpoints
├── db/
│   ├── engine.py        # Async SQLAlchemy engine + get_db dependency
│   ├── orm.py           # ConversationSession / ConversationMessage tables
│   └── repository.py    # Owner-scoped CRUD helpers
├── models/              # Generated request/response models
├── utils/
│   ├── llm.py           # LLM provider selection and clients
│   ├── context.py       # Live patient/appointment grounding
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
- `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` for the sessions database (or `DATABASE_URL` for a full DSN)

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
- `POST /ai/sessions` — start a conversation (optionally bound to a patient/appointment)
- `GET /ai/sessions` — list the caller's sessions
- `GET /ai/sessions/{sessionId}` — fetch a session with its messages
- `DELETE /ai/sessions/{sessionId}` — delete a session
- `POST /ai/sessions/{sessionId}/messages` — send a message and get the reply

Identity headers (`X-User-Id`, `X-User-Role`) are injected by the gateway; only `DOCTOR`/`ADMIN` may use the assistant, and sessions are scoped to their owner.

Example: start a session, then ask a question (the `X-User-*` headers are normally set by the gateway):

```bash
SID=$(curl -s -X POST http://localhost:8000/ai/sessions \
  -H "Content-Type: application/json" \
  -H "X-User-Role: DOCTOR" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" \
  -d '{"patientId": "550e8400-e29b-41d4-a716-446655440000"}' | jq -r .id)

curl -X POST http://localhost:8000/ai/sessions/$SID/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Role: DOCTOR" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" \
  -d '{"query": "Give me a short summary of this patient."}'
```

Add `-H "Accept: text/event-stream"` to the message request to stream the answer token by token.

## Testing

```bash
cd services/ai-assistant
./run_tests.sh
```
