# team-k8s-commanders

Repository for team K8s Commanders.

## About

CareDesk is a clinic management platform. Patients can register, manage their
profile and book appointments with doctors; doctors record clinical notes against
appointments; and an AI assistant answers questions using live patient context.

It is built as a set of microservices — a web client, an API gateway, and auth,
patient, notes, notification and AI-assistant services, each with its own
PostgreSQL database. It runs locally via Docker Compose and on Kubernetes via Helm.

### Project documentation

- [Database schema](docs/DATABASE_SCHEMA.md) documents service-owned databases,
  tables, cross-service references and schema lifecycle.
- [Architecture component diagram](docs/images/Architecture_Component_Diagram.png)
  shows the deployed subsystem decomposition.
- [Deployment diagram](docs/images/Deployment_Diagram.png) shows the runtime
  topology on the AET Kubernetes cluster — pods, databases, and external
  services.
- [Use case diagram](docs/images/Use_Case.png) captures the main patient,
  doctor and administrator workflows.
- [Analysis object model](docs/images/AOM.png) documents the core domain objects
  and relationships.
- [Glossary](docs/GLOSSARY.md) defines shared CareDesk terminology and embeds
  the UML diagrams.

### Team responsibilities

CareDesk was built collaboratively through pull requests and shared review. The
table below records the main ownership areas for the project submission; many
features cross service boundaries and were integrated jointly.

| Student | Main responsibilities |
|---------|-----------------------|
| Florian Behrend | Client Owner |
| Raj Vasani | Server Owner |
| Tobias Klingenberg | GenAI Lead |

### Services

Each backend service has its own README with its endpoints, configuration and how
to build and run it:

| Service | Port | Responsibility |
|---------|------|----------------|
| [api-gateway](services/api-gateway/README.md) | 8080 | Public entry point; verifies JWTs and injects trusted `X-User-*` headers |
| [auth-service](services/auth-service/README.md) | 8081 | User identity, login/JWT issuance, doctor directory |
| [patient-service](services/patient-service/README.md) | 8082 | Appointments, doctor availability, clinics |
| [notes-service](services/notes-service/README.md) | 8083 | Clinical notes per appointment |
| [notification-service](services/notification-service/README.md) | 8084 | Notification records + SMTP delivery and reminders |
| [ai-assistant](services/ai-assistant/README.md) | 8000 | Conversational assistant grounded in live patient context |

The web client lives in [`web-client/`](web-client/); the HTTP contract shared by
all services is [`api/openapi.yaml`](api/openapi.yaml).

## Architecture

CareDesk is a set of independently deployable microservices behind a single
public entry point. Every request from the browser hits one host — Caddy in the
Docker deployments, the Kubernetes ingress on the cluster — which serves the web
client at `/` and forwards `/api/v1/**` to the **API gateway**. Because the
frontend and the API share an origin, no CORS configuration is involved
anywhere.

The gateway is the only component that interprets JWTs. It verifies the token,
strips the `/api/v1` prefix, injects trusted `X-User-Email` / `X-User-Role`
headers, and routes onward:

| Path | Service |
|------|---------|
| `/api/v1/auth/**`, `/api/v1/users/**` | auth-service |
| `/api/v1/patients/**`, `/api/v1/doctors/**`, `/api/v1/appointments/**`, `/api/v1/clinics/**` | patient-service |
| `/api/v1/appointments/*/note` | notes-service |
| `/api/v1/notifications/**`, `/api/v1/appointments/*/notifications` | notification-service |
| `/api/v1/ai/**` | ai-assistant |

Note that `/appointments/**` is deliberately split: the clinical-note and
notification sub-paths go to their owning services while the rest of the
appointment surface stays with patient-service.

### Trusted-header model

Backend services do no JWT parsing of their own — they trust the `X-User-*`
headers the gateway injects. That trust is only sound if nothing else can reach
them, so on Kubernetes the chart ships **NetworkPolicies**: each backend accepts
pod traffic only from the gateway and from the few services allowed to call it
directly, and each Postgres only from the service that owns it. An arbitrary pod
in the namespace cannot reach `caredesk-patient:8082` with forged headers.
Service-to-service calls (patient-service asking notification-service to send a
confirmation, notification-service polling patient-service for upcoming
appointments) use separate `/internal/**` endpoints that the gateway never
exposes and NetworkPolicy restricts to those in-cluster callers. This requires a
CNI that enforces NetworkPolicy — AET's Calico does; a local kind cluster does
not, so the policies are no-ops there.

### Data ownership

Each service owns exactly one PostgreSQL database and no other service reads it
directly. Cross-service relationships are carried as bare UUIDs rather than
foreign keys — an `appointments` row references `users.id` in a database it
cannot reach, so consistency across that boundary is the application's
responsibility, not the database's. See
[docs/DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) for the per-table breakdown.

The [component diagram](docs/images/Architecture_Component_Diagram.png) shows the
same decomposition visually; the [analysis object model](docs/images/AOM.png)
covers the domain objects behind it.

## Local development setup

This project keeps Git hooks and generator tooling in the repository so that a
single setup step is enough to get a working development environment.

### Prerequisites

- Git
- Node.js and npm (LTS recommended)
- Java JDK (required by OpenAPI Generator)
- Optional: Python 3.10+ for running generated FastAPI models locally

### Nix flake (optional)

If you use [Nix](https://nixos.org/), `flake.nix` provides a dev shell with
every prerequisite above (JDK, Maven, Node.js, Python, `docker`) plus the
tooling needed for Kubernetes/infra work (`kubectl`, `helm`, `kind`,
`terraform`, `ansible`):

```bash
nix develop
```

With [direnv](https://direnv.net/) installed, `.envrc` loads this
automatically on `cd` into the repo (run `direnv allow` once). This is purely
an alternative way to get the prerequisites — everything below works the same
whether or not you use it.

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
| Mailpit web UI (caught emails) | http://localhost/mailpit (basic auth: `mailpit` / `mailpit`) |

Only Caddy and the databases publish host ports. The databases are
exposed so you can inspect them locally (e.g. with `psql`); Mailpit's web UI is served through Caddy at `/mailpit` (behind basic auth). The application
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

### Accounts and seeding

**The administrator is always created from env vars** (`CAREDESK_ADMIN_*`, via the bootstrap admin seeder) — independent of demo seeding, in every environment. Dev compose sets these local defaults:

| Role | Email | Password | Source |
|------|-------|----------|--------|
| Admin | admin@admin.com | admin123 | bootstrap env vars (`CAREDESK_ADMIN_*`) |

**Demo users and data** are seeded only when the single demo switch is on — the `dev` Spring profile. It is **always on for local compose** (`SPRING_PROFILES_ACTIVE=dev`) and **off by default on Kubernetes**, where it is toggled by the `SEED_DEMO` GitHub Actions variable on the `AET` environment (set it to `true`, re-run the deploy; the workflow passes it through to the chart's `seedDemoData`). It never runs in production unless explicitly enabled. When on, these demo login accounts exist. **Every patient account uses the password `patient123` and every doctor account uses `doctor123`.**

| Role | Email | Notes |
|------|-------|-------|
| Patient | patient@patient.com | Hypertension history (appointments + notes) |
| Patient | anna.mueller@caredesk.dev | "Anna Müller" — Type 2 diabetes history, the AI-assistant example |
| Patient | max.schmidt@caredesk.dev | General check-up + back-pain history |
| Patient | lena.fischer@caredesk.dev | Asthma history |
| Doctor | doctor@doctor.com | General Medicine — the treating doctor for **all** seeded appointments |
| Doctor | sarah.chen@caredesk.dev | Cardiology — open booking slots only |
| Doctor | tom.becker@caredesk.dev | Pediatrics — open booking slots only |
| Doctor | mark.lopez@caredesk.dev | General Medicine — open booking slots only |

### Demo dataset

The same demo switch also seeds a coherent, cross-service dataset on startup so every dashboard is populated without any manual clicking: appointments across all statuses (including one due within 24h), clinical notes with diagnoses, and notification records. Each of the four demo patients has its own appointment history and notes (hypertension, Type 2 diabetes, a general/back-pain pair, and asthma). All of these appointments are with the General Medicine doctor `doctor@doctor.com`; the three extra doctors (Cardiology, Pediatrics, second General Medicine) currently only provide open slots so the booking dropdown has several specializations to choose from. Seeding is idempotent (fixed UUIDs, upserted) so it survives restarts; disabling the switch stops re-seeding but does not delete already-seeded rows. Log in as `doctor@doctor.com` to see full patient records, schedule and AI context.

> **Warning:** enabling demo seeding creates weak, known-password accounts (above). Keep it off on any public deployment except for a time-boxed demo.

The notes service is a scaffold for clinical notes — the structured visit notes and diagnoses a doctor records against an appointment (`/appointments/{appointmentId}/note`). It follows the same pattern as the patient service: it sits behind the API gateway, trusts the gateway-injected `X-User-Email` / `X-User-Role` headers, and uses its own Postgres container (`notes-db`). The gateway routes the clinical note sub-path to it while the rest of `/appointments/**` stays with the patient service.

The notification service records the automated messages CareDesk sends to patients (appointment confirmations and reminders) and serves them via `/notifications` and `/appointments/{appointmentId}/notifications`. It follows the same pattern as the notes service: it sits behind the API gateway, trusts the gateway-injected `X-User-*` headers, and uses its own Postgres container (`notification-db`). Reads are role-scoped — admins see everything, patients only their own.

The service also delivers those messages by email. We don't run a real mail server: notification-service sends plain SMTP to **Mailpit**, a catch-all container that stores every message and shows it in a web UI reached through Caddy at [http://localhost/mailpit](http://localhost/mailpit) (basic auth `mailpit` / `mailpit` in dev). Pointing at a real provider is purely an `SMTP_*` change. Two things trigger mail:

- **Booking confirmations** — after a patient books, reschedules or cancels, patient-service makes a best-effort call to notification-service, which records the notification and emails the patient. The contact email is captured from the booking request, and a failing or unreachable mail server never blocks the booking.
- **Reminders** — a scheduled job in notification-service polls patient-service for appointments due within the next 24 hours and emails a one-off reminder for each, recorded so the same appointment is never reminded twice (even across restarts).

Both patient-service and notification-service expose internal, gateway-unreachable `/internal/**` endpoints for this service-to-service traffic, restricted to in-cluster callers by NetworkPolicy. On Kubernetes the Helm chart keeps Mailpit ClusterIP-only by default — reach the UI with `kubectl port-forward svc/caredesk-mailpit 8025:8025 -n <namespace>`. To expose it on its own basic-auth ingress instead (as the team's CI deploy does, see the URL above), set `mailpit.ingress.enabled=true` and supply `mailpit.ingress.basicAuth.user` / `.hash`. In the Docker deployments it is fronted by Caddy at `/mailpit`; override `MAILPIT_BASIC_AUTH_USER` / `MAILPIT_BASIC_AUTH_HASH` for any non-local deployment.

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

### Production instances

Live deployments on the AET cluster:

| Instance | URL |
| --- | --- |
| App (web-client) | https://caredesk-team-k8s-commanders.student.k8s.aet.cit.tum.de/ |
| Grafana (monitoring) | https://caredesk-monitoring-team-k8s-commanders.student.k8s.aet.cit.tum.de/ |
| Mailpit (mail catcher) | https://caredesk-mail-team-k8s-commanders.student.k8s.aet.cit.tum.de/ |

Grafana uses its own admin login; Mailpit is behind HTTP basic auth. Credentials
for both come from the team's GitHub Actions variables/secrets.

### One command (no env file)

```bash
helm upgrade --install caredesk helm/caredesk \
  --namespace team-k8s-commanders --create-namespace
```

Open `https://caredesk-team-k8s-commanders.student.k8s.aet.cit.tum.de/`.
cert-manager issues the TLS cert on first deploy (~30 s). The AI assistant
deploys healthy without a key; add one with `--set ai.secrets.llmApiKey=sk-...`.
Full chart docs and routing: [`helm/caredesk/README.md`](helm/caredesk/README.md).

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

## Monitoring

Every Spring service exposes Prometheus metrics at `/actuator/prometheus`; the
AI assistant exposes them at `/metrics`. **Prometheus** scrapes all six on a 15s
interval and **Grafana** renders them — a CareDesk service overview and a JVM
dashboard, plus alert rules covering target-down, 5xx rates, p95 latency, JVM
heap pressure and Hikari connection-pool saturation.

Every firing alert fans out to **two** contact points:

- **Discord** — via webhook, interpolated from `DISCORD_WEBHOOK_URL` at Grafana
  startup so the real URL lives only in the GitHub secret, your local `.env`, or
  the Grafana Kubernetes Secret it's rendered into at deploy time — never in git.
  Without it, a placeholder keeps Grafana booting and alerts still show in the UI.
- **Email into Mailpit** — Grafana's SMTP points at the same Mailpit catch-all
  that receives the application's booking mail (`mailpit:1025` in compose, the
  `caredesk-mailpit` service on Kubernetes), so alerts land in the Mailpit inbox
  too. Handy for demos where Discord isn't wired up. Mailpit accepts any
  recipient, so the `alerts@caredesk.local` address is cosmetic.

Both are sibling routes with `continue: true` on the root notification policy —
Grafana stops at the first matching child otherwise, and a single email route
would silently suppress Discord. Grouped alerts wait 30s before the first
notification, batch follow-ups 5m apart, and re-notify every 4h while firing.

Dashboards, alert rules and contact points live once in
[`infra/grafana/`](infra/grafana/) and are provisioned from there into both
deployments — the compose stack mounts the directory and the Helm chart bakes the
same files into ConfigMaps via symlinks. Edit them in `infra/grafana` and both
environments pick the change up; ad-hoc edits in the Grafana UI are intentionally
not persisted (`editable: false`).

| Environment | Grafana | Prometheus |
|-------------|---------|------------|
| Docker compose | http://localhost/grafana | not published (internal to the compose network) |
| Kubernetes (AET) | https://caredesk-monitoring-team-k8s-commanders.student.k8s.aet.cit.tum.de/ | not exposed — `kubectl port-forward` only |

Prometheus is deliberately never exposed publicly: its API has no
authentication. On Kubernetes the stack runs in **its own namespace**
(`team-k8s-commanders-monitoring`) with its own chart and deploy workflow, so
monitoring upgrades never touch the app. Scrape targets, the namespace/quota
split, alert-webhook setup and troubleshooting are documented in
[`helm/caredesk-monitoring/README.md`](helm/caredesk-monitoring/README.md).

## CI/CD

GitHub Actions covers the whole path from pull request to deployed cluster.

**Per-service CI** runs on every push and pull request, path-filtered so a change
to one service only builds that service. The four data services (auth, patient,
notes, notification) first regenerate the OpenAPI Spring stubs and install them
into the local Maven repo, since they compile against them; all five Spring
services then run `mvn verify` — tests plus **SpotBugs** static analysis. The AI
assistant runs `ruff` and `pytest`; the web client runs lint, tests and a
production build. Failed Java runs upload their surefire and SpotBugs reports as
artifacts (7-day retention).

**Build and publish** ([`publish.yml`](.github/workflows/publish.yml)) runs on
`main`. It first gates on generated-code sync: it re-runs `api/scripts/gen-all.sh`
and fails if the committed Spring stubs or AI-assistant client differ from what
the spec produces — the API-first contract cannot drift unnoticed. Only then does
a build matrix push all seven images to **GHCR**, tagged `latest` and
`sha-<short>`, with layer caching per service.

**Deployment** is chained to that publish run rather than to the commit:

| Workflow | Target | Trigger |
|----------|--------|---------|
| [`deploy-k8s.yml`](.github/workflows/deploy-k8s.yml) | AET cluster (Helm) | successful `publish.yml` run, changes to `helm/caredesk/**`, or manual |
| [`deploy-k8s-monitoring.yml`](.github/workflows/deploy-k8s-monitoring.yml) | AET monitoring namespace (Helm) | changes to `helm/caredesk-monitoring/**` or `infra/grafana/**`, or manual |
| [`deploy-azure.yml`](.github/workflows/deploy-azure.yml) | Azure VM (Docker Compose over SSH) | successful `publish.yml` run, changes to compose/infra files, or manual |

The cluster deploy waits for a **successful** publish and then pins the exact
`sha-<short>` tag that run built, so it can never deploy an image that failed to
build or race a half-pushed `latest`; manual dispatches fall back to `latest`.
The monitoring deploy has no such dependency — it runs upstream Prometheus and
Grafana images, so it never waits on our builds. Both cluster workflows reserve
their namespace's share of the Rancher project quota before deploying, use a
concurrency group to serialise overlapping runs, and dump pods, events and quota
on a failed `helm upgrade`.

Secrets and configuration come from the `AET` and `Azure` GitHub environments.
Neither Helm chart ships a default JWT secret, Postgres password, Grafana
password or Mailpit basic-auth credential — the templates `required` them, and
the workflows fail fast with an explicit error rather than deploying something
publicly reachable with a known password. Demo seeding on the cluster is gated
behind the `SEED_DEMO` variable (see [Accounts and seeding](#accounts-and-seeding)).

**Renovate** ([`renovate.json`](renovate.json)) opens dependency-update pull
requests, which run the same per-service CI before merge.

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
