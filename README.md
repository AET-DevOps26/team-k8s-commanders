# team-k8s-commanders

Repository for team K8s Commanders.

## Local development setup

This project keeps Git hooks and generator tooling in the repository so that a
single setup step is enough to get a working development environment.

### Prerequisites

- Git
- Node.js and npm (LTS recommended)
- Java JDK (required by OpenAPI Generator)
- Optional: Python 3.10+ for running generated FastAPI models locally

### Quick setup

Run the consolidated setup script from the repository root. It will:

- install Node dev dependencies into `node_modules`,
- enable the repo-managed Git hooks, and
- attempt an initial code generation (FastAPI models, Spring server stub,
  TypeScript API types).

```bash
./scripts/setup-all.sh
```

### What the setup script does

- `scripts/setup-generators.sh` — installs Node dev dependencies from
  `package.json`.
- `scripts/install-hooks.sh` — sets `core.hooksPath` to `git/hooks` so the
  versioned shell hooks run automatically.
- `api/scripts/gen-all.sh` — runs OpenAPI Generator and the model generators
  (also invoked by the `post-checkout` and `post-merge` hooks).

## Docker

Start containerized services from the repository root:

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Web client via nginx | http://localhost |
| Web client direct | http://localhost:3000 |
| API gateway | http://localhost:8080 |
| Auth database (Postgres) | localhost:5432 |
| Patient service | http://localhost:8082 |
| Patient database (Postgres) | localhost:5433 |

The web client reads `PUBLIC_API_URL` at runtime (default `/api/v1`) and sends API requests through the gateway. Use `http://localhost` for the full compose setup; nginx serves the frontend and forwards `/api/v1/**` to the API gateway without requiring CORS. Copy `services/ai-assistant/.env.example` to `services/ai-assistant/.env` before the first run if you use the AI assistant service.

The AI assistant uses a local Ollama instance — no additional setup is required. 

```
If Ollama has not been used before on your machine, the first startup may take some time while the model is downloaded.
```

The auth service uses a Postgres container declared in `docker-compose.yml` and ships with a dev-only `JWT_SECRET` baked into the compose file. Override it via the environment for any non-local use. To bring up just the auth stack run `docker compose up auth-service` (this also starts `auth-db`).

The patient service is a scaffold for patient, doctor, appointment and clinic data. It sits behind the API gateway and trusts the `X-User-Email` / `X-User-Role` headers the gateway injects after JWT validation. It uses its own Postgres container (`patient-db`).

See [web-client/README.md](web-client/README.md) for standalone client image builds.

## Useful commands

Re-run generator setup only (no hooks):

```bash
./scripts/setup-generators.sh
```

Install hooks only:

```bash
./scripts/install-hooks.sh
```

Generate artifacts manually:

```bash
./api/scripts/gen-all.sh
```

Run the pre-commit OpenAPI lint hook manually:

```bash
./git/hooks/pre-commit
```

## Where files are written

- Spring server stub: `services/springboot/generated/`
- FastAPI model objects: `services/ai-assistant/models/`
- TypeScript API types: `web-client/src/api.ts`

The `services/*/generated/` and `web-client/src/api.ts` paths are listed in
`.gitignore`. The FastAPI models under `services/ai-assistant/models/` are
checked in so the AI assistant service can import them directly. The models
are directly commited, changes are reflected through the git hooks.

## Troubleshooting

- If generation fails because a tool is missing, verify that `npm` and a Java
  JDK are installed and re-run `./scripts/setup-generators.sh`.
- OpenAPI Generator requires a Java JDK on `PATH`. Install one if a
  Java-related error is reported.

## Updating generator tools

- Node tools: edit `package.json` and run `npm install`.
- For a custom FastAPI implementation, create a separate Python environment
  and import the generated models from `services/ai-assistant/models/`.

## Notes

- Hooks are implemented as shell scripts under `git/hooks` and are
  authoritative; no `pre-commit` YAML is required.
- The OpenAPI specification lives at `api/openapi.yaml` and is the single
  source of truth for all generated clients and server stubs.
