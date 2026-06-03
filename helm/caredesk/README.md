# CareDesk Helm Chart

Deploys the full CareDesk stack — **web-client**, **ai-assistant**, optional **auth-service**, and bundled **PostgreSQL** — to either a **local kind cluster** or the **TUM AET cluster** (Rancher: <https://rancher.ase.cit.tum.de>).

Same chart, same `make` interface. One command for each target.

---

## 0 · Prerequisites

| Tool                   | Local (kind) | AET cluster |
|------------------------|:------------:|:-----------:|
| **Docker** (running)   | ✅ required  | optional    |
| `helm` v3              | auto (brew)  | ✅ required |
| `kubectl`              | auto (brew)  | ✅ required |
| `kind`                 | auto (brew)  | —           |
| TUM cluster kubeconfig | —            | ✅ required |

`scripts/deploy-local.sh` `brew install`s any missing CLI on macOS. On Linux install manually.

---

## 1 · One command — local (kind)

For development, demos, or testing without TUM cluster access.

```bash
make local
```

That's it. The script (idempotent — re-run anytime):

1. Installs `kind` / `kubectl` / `helm` via `brew` if missing
2. Creates a kind cluster `caredesk` with ingress port-mapping (host → **18080/18443**)
3. Installs `ingress-nginx` (kind variant)
4. Builds Docker images for web-client + ai-assistant (and auth-service if its Dockerfile + generated Spring stubs are present)
5. `kind load` images into the cluster (no GHCR pull needed)
6. `helm dependency update` + `helm upgrade --install … --wait`

Open <http://caredesk.localtest.me:18080>. `localtest.me` is a public DNS wildcard pointing to `127.0.0.1` — no `/etc/hosts` edit needed (host ports 18080/18443 avoid the common `8080` clash).

> **If `caredesk.localtest.me` does not resolve** (offline / strict DNS / VPN split-DNS):
> ```bash
> echo "127.0.0.1 caredesk.localtest.me" | sudo tee -a /etc/hosts
> ```
> Or curl-test with a Host header: `curl -H "Host: caredesk.localtest.me" http://localhost:18080/`

**Routes** (single ingress, host `caredesk.localtest.me`):
- `/` → web-client
- `/ai` → ai-assistant
- `/api/v1` → auth-service (only when `auth.enabled=true`)

**Teardown:**
```bash
make local-clean     # deletes the kind cluster (all data wiped)
```

> Requires Docker Desktop / OrbStack running. AI `/ai/query` needs a real `LLM_API_KEY` (default is a dummy); web + auth + DB work out of the box.

---

## 2 · One command — AET TUM cluster

For the tutor / grader.

### Setup (once)

1. **Get kubeconfig from Rancher**
   - Open <https://rancher.ase.cit.tum.de> (TUM VPN required if off-campus)
   - Login with TUM ID → **Cluster (Student Cluster)** → top-right **⋮ → Download KubeConfig** → `stud.yaml`
   - Merge into your `~/.kube/config` (or replace; back up first):
     ```bash
     mkdir -p ~/.kube
     cp ~/.kube/config ~/.kube/config.bak 2>/dev/null || true
     KUBECONFIG=~/.kube/config:~/Downloads/stud.yaml kubectl config view --flatten > /tmp/merged
     mv /tmp/merged ~/.kube/config && chmod 600 ~/.kube/config
     kubectl config use-context stud
     kubectl config current-context        # → stud
     ```

2. **Verify namespace exists** (Rancher provisions it for your team)
   ```bash
   kubectl get ns | grep devops26
   # → e.g. ge38yuc-devops26-team-k8s-commanders   Active
   ```

3. **Fill in deployment env file**
   ```bash
   cp helm/caredesk/.env.k8s.example .env.k8s
   # edit the values described in the table below
   ```

### Deploy

```bash
make deploy
# or, when namespace differs from the default <TUM_ID>-devops26:
NAMESPACE=ge38yuc-devops26-team-k8s-commanders make deploy
```

The script:

1. Loads `.env.k8s`
2. Verifies `kubectl` current-context, creates the namespace if missing
3. `helm repo add bitnami` + `helm dependency update`
4. Runs `helm upgrade --install caredesk helm/caredesk -n <NS> --wait`
5. Prints URLs via `helm get notes`

**Open** the printed URL — e.g. `https://caredesk-<id>.student.k8s.aet.cit.tum.de/`. The chart auto-requests a Let's Encrypt cert via cert-manager (cluster-issuer `letsencrypt-prod`) — first cert issuance takes ~30 s.

> **If Chrome shows `ERR_CERT_AUTHORITY_INVALID` / HSTS warning** on the very first visit (from before cert was issued):
> 1. Open `chrome://net-internals/#hsts`
> 2. "Delete domain security policies" → paste your hostname → **Delete**
> 3. Reload the page (force-refresh: ⌘⇧R)

---

## 3 · `.env.k8s` reference

| Variable              | Required?                | Purpose                                             |
|-----------------------|--------------------------|-----------------------------------------------------|
| `TUM_ID`              | always                   | TUM login id; default namespace becomes `${TUM_ID}-devops26` and ingress host becomes `caredesk-${TUM_ID}.student.k8s.aet.cit.tum.de` |
| `LLM_API_KEY`         | always (ai)              | OpenAI / OpenWebUI key                              |
| `LLM_PROVIDER`        | recommended              | `openai` \| `openwebui`                             |
| `LLM_MODEL`           | recommended              | e.g. `gpt-4o-mini`                                  |
| `OPENWEBUI_BASE_URL`  | when provider=openwebui  | OpenWebUI endpoint                                  |
| `GHCR_USER`           | if GHCR pkgs private     | GitHub username                                     |
| `GHCR_PAT`            | if GHCR pkgs private     | PAT (classic) with `read:packages` scope            |
| `INGRESS_HOST`        | if custom DNS            | Override default ingress host                       |
| `INGRESS_TLS_ENABLED` | optional                 | `true` (default) \| `false`                         |
| `IMAGE_TAG`           | optional                 | Image tag to deploy (default `latest`; CI sets sha) |
| `AUTH_ENABLED`        | optional                 | `false` (default) \| `true` to include auth + DB    |
| `JWT_SECRET`          | only when `AUTH_ENABLED=true` | `openssl rand -hex 32`                         |
| `POSTGRES_PASSWORD`   | only when `AUTH_ENABLED=true` | Strong password for Bitnami Postgres           |

Also reads `NAMESPACE` from shell env (not from `.env.k8s`) to override the default namespace pattern.

---

## 4 · What gets deployed

| Component      | Image (GHCR)                                                  | Port | Replicas | Default state |
|----------------|---------------------------------------------------------------|------|----------|---------------|
| web-client     | `ghcr.io/aet-devops26/team-k8s-commanders/web-client`         | 3000 | 2        | enabled       |
| ai-assistant   | `ghcr.io/aet-devops26/team-k8s-commanders/ai-assistant`       | 8000 | 1        | enabled       |
| auth-service   | `ghcr.io/aet-devops26/team-k8s-commanders/auth-service`       | 8081 | 1        | **disabled** (flip `AUTH_ENABLED=true`) |
| PostgreSQL     | `bitnamilegacy/postgresql:16.4.0-debian-12-r14`               | 5432 | 1        | follows `auth.enabled` |

**Chart side-note**: the web-client image bakes-in an nginx config that proxies `/api/v1/` to a `api-gateway` host (used in `docker-compose.prod.yml`). In Kubernetes this host doesn't exist, so the chart mounts a Kubernetes-only `default.conf` over it via ConfigMap — ingress handles the API routing. No image rebuild needed.

**Routes** (one ingress host):
- `/` → web-client
- `/ai` → ai-assistant
- `/api/v1/…` → auth-service (rewrite-target strips `/api` prefix)

---

## 5 · Useful commands

```bash
# local kind
make local            # full deploy (build + cluster + ingress + helm)
make local-clean      # delete kind cluster

# AET cluster
make deploy           # helm upgrade --install
make undeploy         # helm uninstall (keeps PVCs + namespace)
make purge            # helm uninstall + delete PVCs + delete namespace

# chart dev (no cluster needed for lint/template)
make lint
make template
make dry-run
```

Inspect after deploy:

```bash
NS=ge38yuc-devops26-team-k8s-commanders      # adjust to your namespace
kubectl -n $NS get pods,svc,ingress,certificate
kubectl -n $NS logs -l app.kubernetes.io/instance=caredesk --tail=50 -f
helm get notes caredesk -n $NS
helm get values caredesk -n $NS              # currently active overrides
helm list -n $NS                             # release status

# GUI:
k9s -n $NS                                   # brew install k9s
```

---

## 6 · CI/CD

| Workflow             | Trigger                                  | Action |
|----------------------|------------------------------------------|--------|
| `publish.yml`        | push to `main` touching `services/**` or `web-client/**` | Build + push images to GHCR (matrix: ai-assistant, web-client, auth-service — auth skipped if Dockerfile absent) |
| `deploy-k8s.yml`     | after `Publish Images` succeeds          | `helm upgrade --install` against AET cluster |
| `deploy-azure.yml`   | push to `main` or after publish          | Docker Compose deploy to Azure VM (separate path, see PR #72) |

**Required repo Secrets** (Settings → Secrets and variables → Actions → Secrets):
- `KUBECONFIG_AET` — `base64 < stud.yaml` (one line)
- `GHCR_PULL_PAT` — PAT with `read:packages`
- `LLM_API_KEY` — already exists (PR #72)
- `JWT_SECRET` — only when `AUTH_ENABLED=true`
- `POSTGRES_PASSWORD` — only when `AUTH_ENABLED=true`

**Required repo Variables**:
- `TUM_ID` — e.g. `ge38yuc`
- `AUTH_ENABLED` — `true` | `false`
- `LLM_PROVIDER`, `LLM_MODEL` — already exist (PR #72)
- `INGRESS_HOST` — optional override

---

## 7 · Troubleshooting

### `Error: UPGRADE FAILED: another operation in progress`
Previous `helm install/upgrade` was killed mid-flight → release stuck in `pending-install`. Fix:
```bash
helm history caredesk -n <NS>            # confirm pending status
helm uninstall caredesk -n <NS>          # wipes the release
make deploy                              # fresh install
```

### Web pod `CrashLoopBackOff` with `host not found in upstream "api-gateway"`
Chart should mount the K8s nginx ConfigMap — confirm:
```bash
kubectl -n <NS> get configmap caredesk-web-nginx
kubectl -n <NS> get pod -l app.kubernetes.io/component=web -o yaml | grep -A3 volumeMounts
```
If missing, re-run `make deploy` after pulling latest chart.

### Postgres `ImagePullBackOff: docker.io/bitnami/postgresql … not found`
Bitnami removed legacy tags from docker.io in 2025. Chart pins `bitnamilegacy/postgresql` instead — confirm `values.yaml` has `postgresql.image.repository: bitnamilegacy/postgresql`.

### Auth `CrashLoopBackOff` right after first deploy
Auth waits for Postgres via an `initContainer`. If it still loops, check:
```bash
kubectl -n <NS> logs caredesk-auth-... --previous
```
Usually means `JWT_SECRET` is unquoted in `.env.k8s` (Spring parses it as a number).

### Chrome `ERR_CERT_AUTHORITY_INVALID` / `NET::ERR_CERT_AUTHORITY_INVALID` / HSTS warning
First deploy briefly served a fake cert before cert-manager finished. Browser cached HSTS for it. Fix:
1. `chrome://net-internals/#hsts`
2. "Delete domain security policies" → paste hostname → **Delete**
3. Force-refresh

Verify the live cert is real Let's Encrypt:
```bash
echo | openssl s_client -connect <HOST>:443 -servername <HOST> 2>/dev/null \
  | openssl x509 -noout -issuer
# → issuer=… Let's Encrypt … ✓
```

### Ingress host shows wrong / fake URL
The default rendered host is `caredesk-<TUM_ID>.student.k8s.aet.cit.tum.de`. If the actual cluster ingress uses a different suffix:
1. Open Rancher → your namespace → **Ingresses** tab
2. Note the URL for the `caredesk` ingress
3. Set `INGRESS_HOST` in `.env.k8s` and rerun `make deploy`

### AET cluster gets reset weekly
The DevOps space is wiped end of week (fair-use policy, PDF slide W04E02-14). Re-run `make deploy` after reset — chart is idempotent, no state assumed (PVCs are recreated).
