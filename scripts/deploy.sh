#!/usr/bin/env bash
# Single entrypoint for deploying CareDesk.
#
# Picks the deploy target from the DEPLOY_TARGET environment variable:
#   DEPLOY_TARGET=local  -> scripts/deploy-local.sh  (kind cluster on your machine)
#   DEPLOY_TARGET=aet     -> scripts/deploy-k8s.sh    (AET TUM Rancher cluster)
#
# DEPLOY_TARGET may be set inline, exported, or placed in .env.k8s.
# Defaults to "aet" when unset.
#
# Usage:
#   DEPLOY_TARGET=local ./scripts/deploy.sh
#   DEPLOY_TARGET=aet   ./scripts/deploy.sh
#   make deploy                      # honours DEPLOY_TARGET (default aet)
#   make deploy DEPLOY_TARGET=local  # override via make

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env.k8s"

err() { printf "\033[1;31m[deploy]\033[0m %s\n" "$*" >&2; }

# Allow DEPLOY_TARGET to be defined in .env.k8s without overriding an inline value.
if [[ -z "${DEPLOY_TARGET:-}" && -f "${ENV_FILE}" ]]; then
  DEPLOY_TARGET="$(grep -E '^[[:space:]]*DEPLOY_TARGET=' "${ENV_FILE}" | tail -n1 | cut -d= -f2- | tr -d '"'"'"' ' || true)"
fi

DEPLOY_TARGET="${DEPLOY_TARGET:-aet}"

case "${DEPLOY_TARGET}" in
  local)
    exec "${SCRIPT_DIR}/deploy-local.sh"
    ;;
  aet|cluster|k8s)
    exec "${SCRIPT_DIR}/deploy-k8s.sh"
    ;;
  *)
    err "Unknown DEPLOY_TARGET='${DEPLOY_TARGET}'. Use 'local' or 'aet'."
    exit 1
    ;;
esac
