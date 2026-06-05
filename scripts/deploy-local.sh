#!/usr/bin/env bash
# One-command local deploy of CareDesk to a kind cluster.
#
# Does everything end-to-end (idempotent):
#   1. Verify Docker / brew install kind + kubectl + helm if missing (macOS)
#   2. Create kind cluster `caredesk` with ingress port-mapping (if missing)
#   3. Install ingress-nginx (if missing)
#   4. Build web-client + ai-assistant images (and auth-service if Dockerfile exists)
#   5. kind load images into the cluster
#   6. helm dep update + helm upgrade --install with local overrides
#   7. Print URL
#
# Usage:
#   ./scripts/deploy-local.sh
#   make local

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CHART_DIR="${REPO_ROOT}/helm/caredesk"

CLUSTER_NAME="caredesk"
NAMESPACE="caredesk-local"
RELEASE="caredesk"
IMAGE_TAG="local"
INGRESS_HOST="caredesk.localtest.me"   # resolves to 127.0.0.1
LOCAL_WEB_HOST="localhost"             # avoids proxies that intercept *.localtest.me
INGRESS_PORT="18080"                   # matches helm/caredesk/kind-config.yaml hostPort
REGISTRY="ghcr.io/aet-devops26/team-k8s-commanders"

# ─── Helpers ──────────────────────────────────────────────────────────────────
log()  { printf "\033[1;34m[local]\033[0m %s\n" "$*"; }
warn() { printf "\033[1;33m[local]\033[0m %s\n" "$*"; }
err()  { printf "\033[1;31m[local]\033[0m %s\n" "$*" >&2; }

require_or_install() {
  local bin="$1"
  if command -v "$bin" >/dev/null 2>&1; then return; fi
  if [[ "$(uname -s)" == "Darwin" ]] && command -v brew >/dev/null 2>&1; then
    log "Installing $bin via brew"
    brew install "$bin"
  else
    err "$bin not on PATH. Install it manually and rerun."
    exit 1
  fi
}

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    err "Docker not installed. Install Docker Desktop / OrbStack and start it."
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    err "Docker daemon not running. Start Docker Desktop / OrbStack."
    exit 1
  fi
}

ensure_ingress_host_network() {
  # kind maps host port 18080 to port 80 on the control-plane container.
  # Running ingress-nginx on the node network makes that mapping deterministic;
  # the provider/kind HostPort setup can get stuck after Docker Desktop disk or
  # pod-sandbox resets and then browser requests fail with ERR_CONNECTION_RESET.
  kubectl -n ingress-nginx patch deployment ingress-nginx-controller \
    --type=merge \
    -p '{"spec":{"template":{"spec":{"hostNetwork":true,"dnsPolicy":"ClusterFirstWithHostNet"}}}}' \
    >/dev/null
  kubectl -n ingress-nginx rollout status deploy/ingress-nginx-controller --timeout=180s
}

# ─── 1. Prereqs ───────────────────────────────────────────────────────────────
require_docker
require_or_install kind
require_or_install kubectl
require_or_install helm

# ─── 2. Cluster ───────────────────────────────────────────────────────────────
if kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"; then
  log "kind cluster '${CLUSTER_NAME}' already exists — reusing"
else
  log "Creating kind cluster '${CLUSTER_NAME}'"
  kind create cluster --config "${CHART_DIR}/kind-config.yaml"
fi
kubectl cluster-info --context "kind-${CLUSTER_NAME}" >/dev/null

# ─── 3. Ingress controller ────────────────────────────────────────────────────
if kubectl -n ingress-nginx get deploy ingress-nginx-controller >/dev/null 2>&1; then
  log "ingress-nginx already installed"
else
  log "Installing ingress-nginx (kind variant)"
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
  log "Waiting for ingress-nginx Deployment to roll out (up to 3 min)"
  # Wait for the resource to exist first (apply is async), then for rollout.
  for _ in {1..30}; do
    kubectl -n ingress-nginx get deploy ingress-nginx-controller >/dev/null 2>&1 && break
    sleep 2
  done
  kubectl -n ingress-nginx rollout status deploy/ingress-nginx-controller --timeout=180s
fi
ensure_ingress_host_network

# ─── 4. Build images ──────────────────────────────────────────────────────────
build_image() {
  local svc="$1"
  local ctx="$2"
  local quiet="${3:-false}"
  local build_ctx="${4:-$ctx}"
  if [[ ! -f "${ctx}/Dockerfile" ]]; then
    warn "Skipping ${svc} — ${ctx}/Dockerfile missing"
    return 1
  fi
  log "Building ${svc}:${IMAGE_TAG}"
  if [[ "${quiet}" == "true" ]]; then
    docker build -f "${ctx}/Dockerfile" -t "${REGISTRY}/${svc}:${IMAGE_TAG}" "${build_ctx}" >/dev/null 2>&1
  else
    docker build -f "${ctx}/Dockerfile" -t "${REGISTRY}/${svc}:${IMAGE_TAG}" "${build_ctx}"
  fi
}

# The Spring services (auth/patient/notes) build from the repo root and pull in
# the generated OpenAPI stubs (services/springboot/generated/). Fail fast if they
# are missing — the full stack needs all of them.
if [[ ! -f "${REPO_ROOT}/services/springboot/generated/pom.xml" ]]; then
  err "Generated Spring stubs missing (services/springboot/generated/). Run ./api/scripts/gen-all.sh first."
  exit 1
fi

BUILT_IMAGES=()
#           service          context                                quiet  build-context
build_image web-client      "${REPO_ROOT}/web-client"               false "${REPO_ROOT}" && BUILT_IMAGES+=("${REGISTRY}/web-client:${IMAGE_TAG}")
build_image ai-assistant    "${REPO_ROOT}/services/ai-assistant"                          && BUILT_IMAGES+=("${REGISTRY}/ai-assistant:${IMAGE_TAG}")
build_image api-gateway     "${REPO_ROOT}/services/api-gateway"      true                 && BUILT_IMAGES+=("${REGISTRY}/api-gateway:${IMAGE_TAG}")
build_image auth-service    "${REPO_ROOT}/services/auth-service"     true  "${REPO_ROOT}" && BUILT_IMAGES+=("${REGISTRY}/auth-service:${IMAGE_TAG}")
build_image patient-service "${REPO_ROOT}/services/patient-service"  true  "${REPO_ROOT}" && BUILT_IMAGES+=("${REGISTRY}/patient-service:${IMAGE_TAG}")
build_image notes-service   "${REPO_ROOT}/services/notes-service"    true  "${REPO_ROOT}" && BUILT_IMAGES+=("${REGISTRY}/notes-service:${IMAGE_TAG}")

# ─── 5. Load images into kind ─────────────────────────────────────────────────
# Pull the Postgres base image and load it too, so pullPolicy=Never works for
# every pod (the chart runs one postgres:16-alpine per backend service).
PG_IMAGE="$(grep -E '^[[:space:]]*image:[[:space:]]*postgres' "${CHART_DIR}/values.yaml" | head -n1 | sed -E 's/.*image:[[:space:]]*//; s/["'"'"']//g; s/[[:space:]]*#.*//; s/[[:space:]]*$//')"
PG_IMAGE="${PG_IMAGE:-postgres:16-alpine}"
log "Pulling ${PG_IMAGE} for kind load"
docker pull "${PG_IMAGE}" >/dev/null

log "Loading images into kind cluster"
kind load docker-image "${BUILT_IMAGES[@]}" "${PG_IMAGE}" --name "${CLUSTER_NAME}"

# ─── 6. Helm deploy ───────────────────────────────────────────────────────────
# No subchart dependencies anymore (databases are plain postgres Deployments),
# so no `helm dependency update` / bitnami repo is required.
kubectl get ns "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

SET_FLAGS=(
  --set "tumId=local"
  --set "images.tag=${IMAGE_TAG}"
  --set "images.pullPolicy=Never"               # use kind-loaded images, never pull
  --set "images.pullSecret.create=false"        # no GHCR auth needed
  --set "ingress.host=${INGRESS_HOST}"
  --set "ingress.hostAliases[0]=${LOCAL_WEB_HOST}"
  --set "ingress.tls.enabled=false"             # no cert-manager in kind
  --set "ai.secrets.llmApiKey=local-dummy"      # health probe ok; /ai/query needs real key
  --set "web.replicaCount=1"                    # save resources
  # Relative API base — browser calls stay same-origin whether the user opens
  # localhost:${INGRESS_PORT} or caredesk.localtest.me:${INGRESS_PORT}.
  --set "web.env.publicApiUrl=/api/v1"
)

log "Helm upgrade --install ${RELEASE} -> ns/${NAMESPACE}"
helm upgrade --install "${RELEASE}" "${CHART_DIR}" \
  --namespace "${NAMESPACE}" \
  "${SET_FLAGS[@]}" \
  --wait --timeout 8m

# ─── 7. Done ──────────────────────────────────────────────────────────────────
URL="http://${LOCAL_WEB_HOST}:${INGRESS_PORT}"
log "Deployed."
echo
echo "  Web: ${URL}/"
echo "  API: ${URL}/api/v1/   (via api-gateway)"
echo "       e.g. POST ${URL}/api/v1/auth/register"
echo "  Alt: http://${INGRESS_HOST}:${INGRESS_PORT}/"
echo
echo "Inspect:   kubectl -n ${NAMESPACE} get pods,svc,ingress"
echo "Logs:      kubectl -n ${NAMESPACE} logs -l app.kubernetes.io/instance=${RELEASE} --tail=50"
echo "Teardown:  make local-clean"
