# team-k8s-commanders

Repository for team K8s Commanders.

## About

CareDesk is a clinic management platform. Patients can register, manage their
profile and book appointments with doctors; doctors record clinical notes against
appointments; and an AI assistant answers questions using live patient context.

It is built as a set of microservices — a web client, an API gateway, and auth,
patient, notes and AI-assistant services, each with its own PostgreSQL database.
It runs locally via Docker Compose and on Kubernetes via Helm.

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
| Web client (via Caddy) | http://localhost |
| API gateway | http://localhost:8080 |
| API docs (Swagger UI) | http://localhost/api/v1/docs |
| Grafana (via Caddy) | http://localhost/grafana |
| Auth database (Postgres) | localhost:5432 |
| Patient database (Postgres) | localhost:5433 |
| Notes database (Postgres) | localhost:5434 |
| Notification database (Postgres) | localhost:5435 |
| AI assistant database (Postgres) | localhost:5436 |

Only the gateway, Caddy and the databases publish host ports. The databases are
exposed so you can inspect them locally (e.g. with `psql`). The application
services — web-client, auth-service, patient-service, notes-service and
ai-assistant — publish no host port; they listen only on the internal compose
network and are reached through the gateway (or, for the web client, through
Caddy). The web client reads `PUBLIC_API_URL` at runtime (default `/api/v1`) and sends API requests through the gateway. Use `http://localhost` for the full compose setup; Caddy serves the frontend and forwards `/api/v1/**` to the API gateway without requiring CORS. Copy `services/ai-assistant/.env.example` to `services/ai-assistant/.env` before the first run if you use the AI assistant service.

The AI assistant uses a local Ollama instance — no additional setup is required. 

```
If Ollama has not been used before on your machine, the first startup may take some time while the model is downloaded.
```

The auth service uses a Postgres container declared in `docker-compose.yml` and ships with a dev-only `JWT_SECRET` baked into the compose file. Override it via the environment for any non-local use. To bring up just the auth stack run `docker compose up auth-service` (this also starts `auth-db`).

The patient service manages patient, doctor, appointment and clinic data. It sits behind the API gateway and trusts the `X-User-Email` / `X-User-Role` headers the gateway injects after JWT validation. It uses its own Postgres container (`patient-db`).

### Patient profile and booking flows

Public signup always creates a `PATIENT` account. Doctor and admin creation is intentionally out of scope for public registration and should be handled by admin tooling later.

Patient signup requires:

- full name
- email
- password
- phone number
- date of birth

Authenticated patients can use the web client to open `/patient/profile`, update name, email, phone number and date of birth, and change their password with the current password. The gateway routes `/api/v1/users/**` to auth-service for these account operations.

Patients can open `/patient/book`, search doctors via `/api/v1/doctors`, view available slots via `/api/v1/doctors/{doctorId}/schedule`, and book a selected slot via `/api/v1/appointments`. Booking consumes the selected `doctor_slots` row and marks it unavailable before creating the appointment.

Dev compose seeds these local credentials:

| Role | Email | Password |
|------|-------|----------|
| Patient | patient@patient.com | patient123 |
| Doctor | doctor@doctor.com | doctor123 |
| Admin | admin@admin.com | admin123 |

### Demo dataset

For presentations, dev compose also seeds a coherent, cross-service demo dataset on startup (`CAREDESK_SEED_DEMO=true`, dev profile only — never in production). It populates every dashboard without any manual clicking: appointments across all statuses (including one due within 24h), clinical notes with diagnoses, notification records, and a second demo patient ("Anna Müller", `anna.mueller@caredesk.dev` / `patient123`) with a Type 2 diabetes history for the AI-assistant example. Seeding is idempotent (fixed UUIDs, upserted) so it survives restarts. Log in as the doctor to see full patient records, schedule and AI context.

The notes service is a scaffold for clinical notes — the structured visit notes and diagnoses a doctor records against an appointment (`/appointments/{appointmentId}/note`). It follows the same pattern as the patient service: it sits behind the API gateway, trusts the gateway-injected `X-User-Email` / `X-User-Role` headers, and uses its own Postgres container (`notes-db`). The gateway routes the clinical note sub-path to it while the rest of `/appointments/**` stays with the patient service.

The notification service records the automated messages CareDesk sends to patients (appointment confirmations and reminders) and serves them via `/notifications` and `/appointments/{appointmentId}/notifications`. It follows the same pattern as the notes service: it sits behind the API gateway, trusts the gateway-injected `X-User-*` headers, and uses its own Postgres container (`notification-db`). Reads are role-scoped — admins see everything, patients only their own. Actual email delivery (and the reminder scheduler) is a separate iteration; in this one, notifications are persisted records created via the API.

See [web-client/README.md](web-client/README.md) for standalone client image builds.

## API-driven development

We follow an API-first workflow: the OpenAPI specification at
[`api/openapi.yaml`](api/openapi.yaml) is the single source of truth. The Spring
server stubs, FastAPI models and the TypeScript client types are all generated
from it (see [Local development setup](#local-development-setup)), so the contract
is defined before the code.

The spec is bundled into the API gateway and served alongside an interactive
Swagger UI in every deployment (compose, prod compose and Kubernetes):

- **Swagger UI:** `/api/v1/docs`
- **Raw spec:** `/api/v1/openapi.yaml`

Locally that is http://localhost/api/v1/docs.

## Kubernetes deployment

The whole stack — web-client, api-gateway, auth/patient/notes services,
ai-assistant and one PostgreSQL per service — deploys with **a single command and
no pre-created env files or secrets**. The chart ships working dev defaults and
the GHCR images are public.

### One command (no env file)

```bash
helm upgrade --install caredesk helm/caredesk \
  --namespace <your-namespace> --create-namespace \
  --set tumId=<your-tum-id>
```

Example:

```bash
helm upgrade --install caredesk helm/caredesk \
  --namespace ge38yuc-devops26-team-k8s-commanders --create-namespace \
  --set tumId=ge38yuc
```

Open `https://caredesk-<tumId>.student.k8s.aet.cit.tum.de/`. cert-manager issues
the TLS cert on first deploy (~30 s). The AI assistant deploys healthy without a
key; add one with `--set ai.secrets.llmApiKey=sk-...`. Full chart docs and
routing: [`helm/caredesk/README.md`](helm/caredesk/README.md).

**For the AET cluster** you only need the kubeconfig (context `stud`):

```bash
cp ~/Downloads/stud.yaml ~/.kube/config
kubectl config current-context   # should print: stud
```

**For a local kind cluster**, the `make` wrapper builds + loads images:

```bash
make deploy DEPLOY_TARGET=local   # kind + ingress + helm, optional .env.k8s
```

The `make deploy` / `.env.k8s` path still exists for CI parity and local kind,
but is **optional** — the `helm upgrade --install` above is self-contained.

### Tear down

```bash
helm uninstall caredesk -n <your-namespace>   # removes all resources incl. DB PVCs
make local-clean                              # delete the local kind cluster
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
