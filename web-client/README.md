# CareDesk Web Client

React + TypeScript frontend for CareDesk, built with Vite.

## Prerequisites

- Node.js 20+ (local dev)
- Docker (containerized)

## Local development

```bash
npm install
npm run dev        # http://localhost:3000
```

Set the API URL via `.env.local`:

```env
VITE_API_URL=/api/v1
```

With `npm run dev`, Vite proxies `/api/v1` to the local API gateway on
`http://localhost:8080`, so browser requests stay same-origin.

## Docker

```bash
# Build
docker build -t caredesk-client .

# Run (override API URL at runtime)
docker run -p 3000:3000 \
  -e PUBLIC_API_URL=/api/v1 \
  caredesk-client
```

App available at `http://localhost:3000`.

## Environment variables

| Variable | Where | Description |
|----------|-------|-------------|
| `VITE_API_URL` | build-time (`.env.local` / `--build-arg`) | API base URL for local dev or baked into image |
| `PUBLIC_API_URL` | runtime (`-e` / K8s env) | Overrides build-time URL; default to `/api/v1` behind the gateway |

Runtime `PUBLIC_API_URL` always takes precedence over `VITE_API_URL`.
