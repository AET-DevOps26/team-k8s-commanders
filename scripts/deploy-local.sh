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

BUILT_IMAGES=()
build_image web-client    "${REPO_ROOT}/web-client" false "${REPO_ROOT}" && BUILT_IMAGES+=("${REGISTRY}/web-client:${IMAGE_TAG}")
build_image ai-assistant  "${REPO_ROOT}/services/ai-assistant"           && BUILT_IMAGES+=("${REGISTRY}/ai-assistant:${IMAGE_TAG}")

# auth-service Dockerfile expects build context = repo root and pulls in the
# generated Spring stubs (services/springboot/generated/) which are gitignored.
# Build it from repo root, silently — skip if it fails (e.g. stubs not generated).
AUTH_BUILT=false
if [[ -f "${REPO_ROOT}/services/auth-service/Dockerfile" && -f "${REPO_ROOT}/services/springboot/generated/pom.xml" ]]; then
  if build_image auth-service "${REPO_ROOT}/services/auth-service" true "${REPO_ROOT}"; then
    BUILT_IMAGES+=("${REGISTRY}/auth-service:${IMAGE_TAG}")
    AUTH_BUILT=true
  else
    warn "auth-service build failed — continuing without auth. Run ./api/scripts/gen-all.sh first."
  fi
else
  warn "Skipping auth-service: Dockerfile or generated Spring stubs missing. Run ./api/scripts/gen-all.sh + ensure services/auth-service/Dockerfile exists."
fi

# ─── 5. Load images into kind ─────────────────────────────────────────────────
log "Loading images into kind cluster"
kind load docker-image "${BUILT_IMAGES[@]}" --name "${CLUSTER_NAME}"

# ─── 6. Helm deploy ───────────────────────────────────────────────────────────
log "Helm dependency update"
helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null 2>&1 || true
helm repo update bitnami >/dev/null 2>&1 || warn "bitnami repo update failed — using cached chart if present"
# Only run dep update if subchart not yet downloaded — avoids hard failure when bitnami is unreachable.
if [[ ! -d "${CHART_DIR}/charts" ]] || ! ls "${CHART_DIR}/charts"/postgresql-*.tgz >/dev/null 2>&1; then
  helm dependency update "${CHART_DIR}"
else
  log "Postgres subchart already present — skipping dep update"
fi

kubectl get ns "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

SET_FLAGS=(
  --set "tumId=local"
  --set "images.tag=${IMAGE_TAG}"
  --set "images.pullPolicy=Never"               # use kind-loaded images, never pull
  --set "images.pullSecret.create=false"        # no GHCR auth needed
  --set "ingress.host=${INGRESS_HOST}"
  --set "ingress.tls.enabled=false"             # no cert-manager in kind
  --set "ai.secrets.llmApiKey=local-dummy"      # health probe ok; /ai/query needs real key
  --set "web.replicaCount=1"                    # save resources
)

if [[ "${AUTH_BUILT}" == "true" ]]; then
  log "auth-service Dockerfile found — enabling auth + Postgres"
  SET_FLAGS+=(
    --set "auth.enabled=true"
    --set "postgresql.enabled=true"
    --set "auth.secrets.jwtSecret=local-jwt-secret-do-not-use-in-prod"
    --set "postgresql.auth.password=localpass"
    --set "postgresql.primary.persistence.size=512Mi"
  )
fi

log "Helm upgrade --install ${RELEASE} -> ns/${NAMESPACE}"
helm upgrade --install "${RELEASE}" "${CHART_DIR}" \
  --namespace "${NAMESPACE}" \
  "${SET_FLAGS[@]}" \
  --wait --timeout 5m

# ─── 7. Done ──────────────────────────────────────────────────────────────────
URL="http://${INGRESS_HOST}:${INGRESS_PORT}"
log "Deployed."
echo
echo "  Web: ${URL}/"
echo "  AI:  ${URL}/ai/health"
if [[ "${AUTH_BUILT}" == "true" ]]; then
  echo "  API: ${URL}/api/auth/login"
fi
echo
echo "Inspect:   kubectl -n ${NAMESPACE} get pods,svc,ingress"
echo "Logs:      kubectl -n ${NAMESPACE} logs -l app.kubernetes.io/instance=${RELEASE} --tail=50"
echo "Teardown:  make local-clean"
