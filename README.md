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
| Web client | http://localhost:3000 |
| AI assistant | http://localhost:8000 |
| Auth service | http://localhost:8081 |
| Auth database (Postgres) | localhost:5432 |

The web client reads `PUBLIC_API_URL` at runtime (default `http://host.docker.internal:8080` for the Spring API on the host). Copy `services/ai-assistant/.env.example` to `services/ai-assistant/.env` before the first run if you use the AI assistant service.

The AI assistant uses a local Ollama instance — no additional setup is required. 

```
If Ollama has not been used before on your machine, the first startup may take some time while the model is downloaded.
```

The auth service uses a Postgres container declared in `docker-compose.yml` and ships with a dev-only `JWT_SECRET` baked into the compose file. Override it via the environment for any non-local use. To bring up just the auth stack run `docker compose up auth-service` (this also starts `auth-db`).

See [web-client/README.md](web-client/README.md) for standalone client image builds.

## Kubernetes deployment

The app deploys to Kubernetes via a single command. One environment variable,
`DEPLOY_TARGET`, decides **where** it lands:

| `DEPLOY_TARGET` | Where | Script |
|-----------------|-------|--------|
| `local` | A [kind](https://kind.sigs.k8s.io/) cluster on your own machine | `scripts/deploy-local.sh` |
| `aet` (default) | The AET TUM Rancher cluster | `scripts/deploy-k8s.sh` |

### One command

```bash
make deploy                      # uses DEPLOY_TARGET from .env.k8s (default: aet)
make deploy DEPLOY_TARGET=local  # force local kind
make deploy DEPLOY_TARGET=aet    # force AET cluster
```

`DEPLOY_TARGET` is resolved in this order: an inline/`make` value wins, then an
exported shell variable, then the `DEPLOY_TARGET=` line in `.env.k8s`, otherwise
it falls back to `aet`. `make local` is just a shortcut for
`make deploy DEPLOY_TARGET=local`.

### First-time setup

1. **Config file.** Copy the template and fill it in:

   ```bash
   cp .env.k8s.example .env.k8s
   ```

   `.env.k8s` is gitignored. Set at least:
   - `DEPLOY_TARGET` — `local` or `aet`.
   - `TUM_ID` — your LRZ/TUM kennung. The namespace is `<TUM_ID>-devops26`. It
     **must** be a namespace your Rancher account owns. Verify with
     `kubectl auth can-i --list`; any namespace listed with verb `*` is yours.
   - `LLM_API_KEY` — required (a real key for `/ai/query`; any non-empty value
     passes health checks).
   - `GHCR_USER` / `GHCR_PAT` — only if the container images are private (PAT
     scope: `read:packages`). Leave empty for public images.

2. **For `aet` only — kubeconfig.** Download `stud.yaml` from
   <https://rancher.ase.cit.tum.de> and place it at `~/.kube/config`:

   ```bash
   cp ~/Downloads/stud.yaml ~/.kube/config
   kubectl config current-context   # should print: stud
   ```

3. **For `local` only — Docker.** Docker Desktop / OrbStack must be running;
   `kind`, `kubectl`, and `helm` are auto-installed via brew on macOS if missing.

### Tear down

```bash
make undeploy   # uninstall release, keep PVCs + namespace
make purge      # uninstall + delete PVCs + delete namespace
make local-clean  # delete the local kind cluster entirely
```

### Chart utilities (no live cluster needed)

```bash
make lint       # helm lint
make template   # render manifests to stdout
make dry-run    # helm upgrade --dry-run against the current cluster
```

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
