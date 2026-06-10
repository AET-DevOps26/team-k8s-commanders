#!/usr/bin/env bash
# One-command deploy of CareDesk to the AET Kubernetes cluster.
#
# Works with ZERO configuration — the chart ships dev defaults and the GHCR
# images are public, so no .env.k8s and no secrets are required:
#
#   ./scripts/deploy-k8s.sh
#   make deploy
#
# Prerequisites:
#   - helm v3 + kubectl + python3 on PATH
#   - kubeconfig at ~/.kube/config (download stud.yaml from https://rancher.ase.cit.tum.de)
#
# Optional overrides (env or .env.k8s at repo root): TUM_ID, RELEASE,
# IMAGE_TAG, LLM_API_KEY, LLM_PROVIDER, LLM_MODEL, OPENWEBUI_BASE_URL,
# JWT_SECRET, POSTGRES_PASSWORD, INGRESS_HOST, INGRESS_TLS_ENABLED,
# GHCR_USER, GHCR_PAT.

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
require python3

# .env.k8s is OPTIONAL — load it only if present (overrides defaults below).
if [[ -f "${ENV_FILE}" ]]; then
  log "Loading overrides from .env.k8s"
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

# Defaults make the script runnable with no config at all.
TUM_ID="${TUM_ID:-ge38yuc}"
NAMESPACE="${TUM_ID}-devops26-team-k8s-commanders"
RELEASE="${RELEASE:-caredesk}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

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

bash "${SCRIPT_DIR}/check-k8s-quota.sh" "${NAMESPACE}"

# ─── Compose --set flags ───────────────────────────────────────────────────────
# The full stack (web, gateway, auth, patient, notes, ai + one Postgres each)
# is enabled by chart defaults. Everything below is an OPTIONAL override.
SET_FLAGS=(
  --set "tumId=${TUM_ID}"
  --set "images.tag=${IMAGE_TAG}"
)
[[ -n "${LLM_API_KEY:-}" ]]         && SET_FLAGS+=(--set "ai.secrets.llmApiKey=${LLM_API_KEY}")
[[ -n "${LLM_PROVIDER:-}" ]]        && SET_FLAGS+=(--set "ai.env.llmProvider=${LLM_PROVIDER}")
[[ -n "${LLM_MODEL:-}" ]]           && SET_FLAGS+=(--set "ai.env.llmModel=${LLM_MODEL}")
[[ -n "${OPENWEBUI_BASE_URL:-}" ]]  && SET_FLAGS+=(--set "ai.env.openwebuiBaseUrl=${OPENWEBUI_BASE_URL}")
[[ -n "${JWT_SECRET:-}" ]]          && SET_FLAGS+=(--set "backend.jwtSecret=${JWT_SECRET}")
[[ -n "${POSTGRES_PASSWORD:-}" ]]   && SET_FLAGS+=(--set "postgres.password=${POSTGRES_PASSWORD}")
[[ -n "${GHCR_USER:-}" ]]           && SET_FLAGS+=(--set "images.pullSecret.create=true" --set "images.pullSecret.username=${GHCR_USER}")
[[ -n "${GHCR_PAT:-}" ]]            && SET_FLAGS+=(--set "images.pullSecret.password=${GHCR_PAT}")
[[ -n "${INGRESS_HOST:-}" ]]        && SET_FLAGS+=(--set "ingress.host=${INGRESS_HOST}")
[[ -n "${INGRESS_TLS_ENABLED:-}" ]] && SET_FLAGS+=(--set "ingress.tls.enabled=${INGRESS_TLS_ENABLED}")

if [[ -z "${LLM_API_KEY:-}" ]]; then
  warn "No LLM_API_KEY set — ai-assistant deploys healthy but cannot answer until a key is provided."
fi

# ─── Deploy ───────────────────────────────────────────────────────────────────
# 8 Spring/Node pods + 3 Postgres on a shared student cluster need a generous
# --wait window for image pulls and JPA schema creation on first boot.
log "helm upgrade --install ${RELEASE} -> ns/${NAMESPACE}"
helm upgrade --install "${RELEASE}" "${CHART_DIR}" \
  --namespace "${NAMESPACE}" \
  "${SET_FLAGS[@]}" \
  --wait --timeout 9m

log "Done. Release notes:"
helm get notes "${RELEASE}" --namespace "${NAMESPACE}"
