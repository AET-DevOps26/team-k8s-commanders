# CareDesk Helm Chart

Deploys the full CareDesk stack — **web-client**, **ai-assistant**, optional **auth-service**, and bundled **PostgreSQL** — either to a **local kind cluster** or to the **TUM AET cluster** (Rancher: <https://rancher.ase.cit.tum.de>).

Same chart, same `make` interface. One command for either target.

---

## One command — local (kind)

For development, demos, and testing without TUM cluster access.

```bash
make local
```

That's literally it. The script will (idempotently):

1. Install `kind`, `kubectl`, `helm` via `brew` if missing
2. Create a kind cluster `caredesk` with ingress port-mapping (host → 18080/18443)
3. Install `ingress-nginx` (kind variant)
4. Build Docker images for web-client + ai-assistant (and auth-service if its Dockerfile exists)
5. `kind load` images into the cluster (no GHCR pull needed)
6. `helm dependency update` + `helm upgrade --install … --wait`

Then open <http://caredesk.localtest.me:18080>. (`localtest.me` resolves to `127.0.0.1` — no `/etc/hosts` edit needed. Host ports 18080/18443 chosen to avoid the common `8080` clash.)

> If `caredesk.localtest.me` does not resolve (offline / strict DNS), add it to `/etc/hosts`:
> ```bash
> echo "127.0.0.1 caredesk.localtest.me" | sudo tee -a /etc/hosts
> ```
> Or test with a Host header: `curl -H "Host: caredesk.localtest.me" http://localhost:18080/`

Teardown:
```bash
make local-clean      # delete the kind cluster
```

> Requires Docker Desktop / OrbStack running. AI `/ai/query` needs a real `LLM_API_KEY` (default is a dummy); the rest works out of the box.

---

## One command — AET TUM cluster

For the tutor / grader.

```bash
# 1. Download stud.yaml from Rancher and place it at ~/.kube/config
#    (Rancher → Cluster → ⋮ → Download KubeConfig)
kubectl config current-context   # should print "stud"

# 2. Copy and fill in deployment env file
cp helm/caredesk/.env.k8s.example .env.k8s
# edit TUM_ID, GHCR_USER, GHCR_PAT, LLM_API_KEY (and JWT/Postgres if AUTH_ENABLED=true)

# 3. Deploy
make deploy
```

The script will:

1. Create the namespace `<TUM_ID>-devops26` if missing
2. `helm repo add bitnami` + `helm dependency update`
3. Render Helm values from `.env.k8s` and run `helm upgrade --install … --wait`
4. Print the application URL via `helm get notes`

---

## What gets deployed

| Component      | Image (GHCR)                                                   | Port | Replicas | Default state |
|----------------|----------------------------------------------------------------|------|----------|---------------|
| web-client     | `ghcr.io/aet-devops26/team-k8s-commanders/web-client`          | 3000 | 2        | enabled       |
| ai-assistant   | `ghcr.io/aet-devops26/team-k8s-commanders/ai-assistant`        | 8000 | 1        | enabled       |
| auth-service   | `ghcr.io/aet-devops26/team-k8s-commanders/auth-service`        | 8081 | 1        | **disabled** (flip `AUTH_ENABLED=true` once Dockerfile exists) |
| PostgreSQL     | Bitnami subchart                                               | 5432 | 1        | follows `auth.enabled` |

Single Ingress hostname routes:
- `/`     → web-client
- `/api`  → auth-service (when enabled)
- `/ai`   → ai-assistant

---

## Configuration

All knobs live in [`values.yaml`](values.yaml). Override on the CLI:

```bash
helm upgrade --install caredesk helm/caredesk \
  --namespace <tum-id>-devops26 \
  --set tumId=<tum-id> \
  --set ai.secrets.llmApiKey=sk-... \
  --set images.pullSecret.username=<gh-user> \
  --set images.pullSecret.password=<ghcr-pat>
```

Or use a values override file:

```bash
cp helm/caredesk/values.example.yaml values.local.yaml   # gitignored
helm upgrade --install caredesk helm/caredesk -n <ns> -f values.local.yaml
```

### Required values

| Key                                       | Required when         | How to provide                           |
|-------------------------------------------|-----------------------|------------------------------------------|
| `tumId`                                   | always                | `.env.k8s` → `TUM_ID`                    |
| `ai.secrets.llmApiKey`                    | `ai.enabled=true`     | `.env.k8s` → `LLM_API_KEY`               |
| `images.pullSecret.username/password`     | GHCR pkg is private   | `.env.k8s` → `GHCR_USER` + `GHCR_PAT`    |
| `auth.secrets.jwtSecret`                  | `auth.enabled=true`   | `.env.k8s` → `JWT_SECRET`                |
| `postgresql.auth.password`                | `postgresql.enabled`  | `.env.k8s` → `POSTGRES_PASSWORD`         |

### Ingress hostname

Default rendered host: `caredesk-<tumId>.student.k8s.aet.cit.tum.de`.

If that doesn't resolve, find the actual host:
1. Open <https://rancher.ase.cit.tum.de>
2. Cluster → Namespace `<tum-id>-devops26` → **Ingresses** tab
3. Note the URL shown for the `caredesk` ingress
4. Override:
   ```bash
   helm upgrade caredesk helm/caredesk -n <ns> --reuse-values \
     --set ingress.host=<actual-host>
   ```

---

## Useful commands

```bash
make lint        # helm lint
make template    # render manifests (no cluster needed)
make dry-run     # helm install --dry-run --debug
make deploy      # production deploy
make undeploy    # helm uninstall, keep PVCs + namespace
make purge       # helm uninstall + delete PVCs + delete namespace
```

Inspect after deploy:

```bash
kubectl -n <tum-id>-devops26 get pods,svc,ingress
kubectl -n <tum-id>-devops26 logs -l app.kubernetes.io/instance=caredesk --tail=50
helm get notes caredesk -n <tum-id>-devops26
```

---

## CI/CD

Two GitHub Actions handle automation:

| Workflow                | Trigger                                  | Action |
|-------------------------|------------------------------------------|--------|
| `publish.yml`           | push to `main` touching services         | Build + push images to GHCR (matrix: ai-assistant, web-client, auth-service) |
| `deploy-k8s.yml`        | after `Publish Images` succeeds          | `helm upgrade --install` against AET cluster |

Required repository configuration:

**Secrets** (Settings → Secrets and variables → Actions → Secrets):
- `KUBECONFIG_AET` — base64 of `stud.yaml`
- `GHCR_PULL_PAT` — PAT with `read:packages`
- `LLM_API_KEY` — already exists from PR #72
- `JWT_SECRET` — only when `AUTH_ENABLED=true`
- `POSTGRES_PASSWORD` — only when `AUTH_ENABLED=true`

**Variables** (Settings → Secrets and variables → Actions → Variables):
- `TUM_ID` — e.g. `ge38yuc`
- `AUTH_ENABLED` — `true` or `false`
- `LLM_PROVIDER`, `LLM_MODEL` — already exist from PR #72
- `INGRESS_HOST` — optional override
