#!/usr/bin/env bash
# One-command deploy of CareDesk to the AET Kubernetes cluster.
#
# Prerequisites (the tutor):
#   - helm v3 + kubectl on PATH
#   - kubeconfig at ~/.kube/config (download stud.yaml from https://rancher.ase.cit.tum.de)
#   - .env.k8s at repo root, filled in from helm/caredesk/.env.k8s.example
#
# Usage:
#   ./scripts/deploy-k8s.sh
#   make deploy

set -euo pipefail

# ─── Locate repo root ─────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CHART_DIR="${REPO_ROOT}/helm/caredesk"
ENV_FILE="${REPO_ROOT}/.env.k8s"

# ─── Helpers ───────────────────────────────────────────────────────────────────
log()  { printf "\033[1;34m[deploy]\033[0m %s\n" "$*"; }
warn() { printf "\033[1;33m[deploy]\033[0m %s\n" "$*"; }
err()  { printf "\033[1;31m[deploy]\033[0m %s\n" "$*" >&2; }

require() {
  command -v "$1" >/dev/null 2>&1 || { err "$1 not on PATH. Install it first."; exit 1; }
}

# ─── Preflight ─────────────────────────────────────────────────────────────────
require helm
require kubectl

if [[ ! -f "${ENV_FILE}" ]]; then
  err ".env.k8s not found at ${ENV_FILE}"
  err "Create one: cp ${CHART_DIR}/.env.k8s.example ${ENV_FILE} && edit it."
  exit 1
fi

# Load .env.k8s (export every assignment)
set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

: "${TUM_ID:?TUM_ID is required in .env.k8s}"
: "${LLM_API_KEY:?LLM_API_KEY is required in .env.k8s}"
# NAMESPACE overridable (AET cluster sometimes provisions namespaces with
# different naming, e.g. <tumId>-devops26-<team>). Default keeps PDF pattern.
NAMESPACE="${NAMESPACE:-${TUM_ID}-devops26}"
RELEASE="${RELEASE:-caredesk}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
AUTH_ENABLED="${AUTH_ENABLED:-false}"

# Verify kube context
if ! kubectl config current-context >/dev/null 2>&1; then
  err "kubectl has no current-context. Place stud.yaml at ~/.kube/config and retry."
  exit 1
fi
log "kube context: $(kubectl config current-context)"

# ─── Namespace ────────────────────────────────────────────────────────────────
log "Ensuring namespace ${NAMESPACE}"
kubectl get ns "${NAMESPACE}" >/dev/null 2>&1 || \
  kubectl create namespace "${NAMESPACE}"

# ─── Helm dependencies (Bitnami Postgres) ─────────────────────────────────────
log "Adding bitnami repo + updating chart dependencies"
helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null 2>&1 || true
helm repo update bitnami >/dev/null
helm dependency update "${CHART_DIR}" >/dev/null

# ─── Compose --set flags ───────────────────────────────────────────────────────
SET_FLAGS=(
  --set "tumId=${TUM_ID}"
  --set "images.tag=${IMAGE_TAG}"
  --set "ai.secrets.llmApiKey=${LLM_API_KEY}"
)
[[ -n "${LLM_PROVIDER:-}" ]]      && SET_FLAGS+=(--set "ai.env.llmProvider=${LLM_PROVIDER}")
[[ -n "${LLM_MODEL:-}" ]]         && SET_FLAGS+=(--set "ai.env.llmModel=${LLM_MODEL}")
[[ -n "${OPENWEBUI_BASE_URL:-}" ]] && SET_FLAGS+=(--set "ai.env.openwebuiBaseUrl=${OPENWEBUI_BASE_URL}")
[[ -n "${GHCR_USER:-}" ]]         && SET_FLAGS+=(--set "images.pullSecret.username=${GHCR_USER}")
[[ -n "${GHCR_PAT:-}" ]]          && SET_FLAGS+=(--set "images.pullSecret.password=${GHCR_PAT}")
[[ -n "${INGRESS_HOST:-}" ]]      && SET_FLAGS+=(--set "ingress.host=${INGRESS_HOST}")
[[ -n "${INGRESS_TLS_ENABLED:-}" ]] && SET_FLAGS+=(--set "ingress.tls.enabled=${INGRESS_TLS_ENABLED}")

if [[ "${AUTH_ENABLED}" == "true" ]]; then
  : "${JWT_SECRET:?JWT_SECRET required when AUTH_ENABLED=true}"
  : "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD required when AUTH_ENABLED=true}"
  SET_FLAGS+=(
    --set "auth.enabled=true"
    --set "postgresql.enabled=true"
    --set "auth.secrets.jwtSecret=${JWT_SECRET}"
    --set "postgresql.auth.password=${POSTGRES_PASSWORD}"
  )
else
  warn "auth.enabled=false (set AUTH_ENABLED=true in .env.k8s to include auth-service + Postgres)"
fi

# ─── Deploy ───────────────────────────────────────────────────────────────────
log "helm upgrade --install ${RELEASE} -> ns/${NAMESPACE}"
helm upgrade --install "${RELEASE}" "${CHART_DIR}" \
  --namespace "${NAMESPACE}" \
  "${SET_FLAGS[@]}" \
  --wait --timeout 5m

log "Done. Release notes:"
helm get notes "${RELEASE}" --namespace "${NAMESPACE}"
