# CareDesk Helm Chart

Deploys the **full CareDesk stack** to a Kubernetes cluster (local **kind** or the
**TUM AET** Rancher cluster):

- **web-client** (React + nginx)
- **api-gateway** (Spring Cloud Gateway — single API entry point, JWT verify, header injection)
- **auth-service**, **patient-service**, **notes-service** (Spring Boot)
- **ai-assistant** (FastAPI)
- one **PostgreSQL** per backend service (`auth-db`, `patient-db`, `notes-db`)

---

## 1 · Deploy with one command — no env files

The chart ships working development defaults (JWT secret, DB password) and the
GHCR images are **public**, so it installs with a **single command and zero
pre-created files or secrets** — exactly what a grader needs.

```bash
helm upgrade --install caredesk helm/caredesk \
  --namespace <your-namespace> --create-namespace \
  --set tumId=<your-tum-id>
```

That is the whole deploy. Example for this team:

```bash
helm upgrade --install caredesk helm/caredesk \
  --namespace ge38yuc-devops26-team-k8s-commanders --create-namespace \
  --set tumId=ge38yuc
```

- `tumId` only drives the default ingress host
  `caredesk-<tumId>.student.k8s.aet.cit.tum.de`. Override the host directly with
  `--set ingress.host=<host>` if it differs (look it up in Rancher → Ingresses).
- The AI assistant **deploys and stays healthy without a key**; it just cannot
  answer until you add one: `--set ai.secrets.llmApiKey=sk-...`.
- For a real environment, override the dev defaults:
  `--set backend.jwtSecret=<secret> --set postgres.password=<pw>`.

Open the printed URL (`https://caredesk-<tumId>.student.k8s.aet.cit.tum.de/`).
cert-manager (`letsencrypt-prod`) issues the TLS cert on first deploy (~30 s).

### Prerequisites

| Tool | Local (kind) | AET cluster |
|------|:---:|:---:|
| `helm` v3 | ✅ | ✅ |
| `kubectl` | ✅ | ✅ |
| `kind` + Docker | ✅ | — |
| AET kubeconfig (`stud.yaml` → `~/.kube/config`, context `stud`) | — | ✅ |

---

## 2 · What gets deployed

| Component | Image (`ghcr.io/aet-devops26/team-k8s-commanders/…`) | Port | Replicas |
|-----------|------------------------------------------------------|------|----------|
| web-client | `web-client` | 3000 | 2 |
| api-gateway | `api-gateway` | 8080 | 1 |
| auth-service | `auth-service` | 8081 | 1 |
| patient-service | `patient-service` | 8082 | 1 |
| notes-service | `notes-service` | 8083 | 1 |
| ai-assistant | `ai-assistant` | 8000 | 1 |
| auth-db / patient-db / notes-db | `postgres:16-alpine` | 5432 | 1 each (1Gi PVC) |

Images default to tag `latest`; override with `--set images.tag=<tag>`.

**Routing** (single ingress host):

- `/` → **web-client**
- `/api/v1/**` → **api-gateway**, which verifies the JWT, injects `X-User-*`
  headers and routes onward:
  - `/api/v1/auth/**` → auth-service
  - `/api/v1/patients/**`, `/api/v1/appointments/**`, `/api/v1/doctors/**` → patient-service
  - `/api/v1/appointments/*/note` → notes-service
  - `/api/v1/ai/**` → ai-assistant

The web-client calls the API at `https://<host>/api/v1` (injected at runtime via
`PUBLIC_API_URL` → `window.__ENV__`). No `api-gateway` DNS dependency inside the
image — the chart mounts a Kubernetes-only nginx config.

---

## 3 · Tear down

```bash
helm uninstall caredesk -n <your-namespace>
```

This removes every chart resource **including the database PVCs** (they are
chart-owned, so their data is wiped). The namespace itself is left in place.

---

## 4 · Chart utilities (no live cluster needed)

```bash
helm lint helm/caredesk
helm template caredesk helm/caredesk -n <ns>              # render manifests
helm template caredesk helm/caredesk -n <ns> | kubectl apply --dry-run=server -f -
```

A `Makefile` wrapper (`make deploy` / `make undeploy`, driven by an optional
`.env.k8s`) is also available for CI parity, but is **not required** — the single
`helm upgrade --install` above is self-contained.

---

## 5 · CI/CD

| Workflow | Trigger | Action |
|----------|---------|--------|
| `publish.yml` | push to `main` touching `services/**` or `web-client/**` | Build + push all 6 images to GHCR (matrix: web-client, api-gateway, auth-service, patient-service, notes-service, ai-assistant) |
| `deploy-k8s.yml` | after Publish Images succeeds | `helm upgrade --install` against the AET cluster (image tag = `sha-<short>`) |

**Required repo secrets/variables** for the CI deploy: `KUBECONFIG_AET`,
`TUM_ID`, and (optionally) `LLM_API_KEY`. JWT secret and DB password fall back to
the chart's dev defaults unless overridden.

---

## 6 · Troubleshooting

### `helm uninstall` left PVCs behind
The chart deletes its PVCs, but a stuck finalizer can leave them `Terminating`.
Force-remove:
```bash
kubectl -n <ns> delete pvc -l app.kubernetes.io/part-of=caredesk
```

### api-gateway `CrashLoopBackOff` — "Spring Boot 3.5.x not compatible with this Spring Cloud release train"
The published image pins Spring Cloud 2023.0.x under Spring Boot 3.5.x; the strict
compatibility verifier aborts startup even though the gateway runs fine at runtime.
The chart already disables the verifier
(`SPRING_CLOUD_COMPATIBILITYVERIFIER_ENABLED=false`). The permanent fix is the
gateway `pom.xml` bump to `spring-cloud.version` **2025.0.x** (aligned with Boot
3.5); once a new image is published the flag becomes a harmless no-op.

### Spring service `CrashLoopBackOff` on first boot
Each service waits on its own Postgres and creates its schema
(`SPRING_JPA_HIBERNATE_DDL_AUTO=update`). The startup probe allows ~200 s. If it
still loops, check the DB pod and logs:
```bash
kubectl -n <ns> get pods
kubectl -n <ns> logs deploy/caredesk-<service> --previous
```

### Ingress host shows the wrong URL
Default host is `caredesk-<tumId>.student.k8s.aet.cit.tum.de`. If the cluster uses
a different suffix, look it up in Rancher → Ingresses and redeploy with
`--set ingress.host=<actual-host>`.

### AET cluster gets reset weekly
The DevOps namespace is wiped end of week. Re-run the one-command deploy — the
chart is idempotent and assumes no pre-existing state (PVCs are recreated).
