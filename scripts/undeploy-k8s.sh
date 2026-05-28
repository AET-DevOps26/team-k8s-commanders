#!/usr/bin/env bash
# Tear down the CareDesk release. By default keeps the namespace + PVCs.
# Use --purge to also delete PVCs (Bitnami Postgres data) and the namespace.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env.k8s"

log()  { printf "\033[1;34m[undeploy]\033[0m %s\n" "$*"; }

PURGE=false
[[ "${1:-}" == "--purge" ]] && PURGE=true

if [[ -f "${ENV_FILE}" ]]; then
  set -a; source "${ENV_FILE}"; set +a
fi
: "${TUM_ID:?TUM_ID is required (set in .env.k8s or env)}"
NAMESPACE="${TUM_ID}-devops26"
RELEASE="${RELEASE:-caredesk}"

log "Uninstalling release ${RELEASE} from ns/${NAMESPACE}"
helm uninstall "${RELEASE}" --namespace "${NAMESPACE}" --ignore-not-found

if [[ "${PURGE}" == "true" ]]; then
  log "Purging PVCs in ns/${NAMESPACE}"
  kubectl delete pvc --all --namespace "${NAMESPACE}" --ignore-not-found
  log "Deleting namespace ${NAMESPACE}"
  kubectl delete namespace "${NAMESPACE}" --ignore-not-found
fi
log "Done."
