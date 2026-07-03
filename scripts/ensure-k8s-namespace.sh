#!/usr/bin/env bash
# Ensure a namespace exists, belongs to the team's Rancher project, and (optionally)
# declares its share of the project quota.
#
# Usage: RANCHER_PROJECT_ID=<cluster-id>:<project-id> \
#          ensure-k8s-namespace.sh <namespace> [limits-cpu] [limits-memory]
#
# Rancher assigns namespaces to projects via the field.cattle.io/projectId
# annotation (e.g. "c-m-abc12def:p-xyz45" — both IDs are visible in the Rancher
# URL while the project is open). Applying the annotation at creation time puts
# the namespace straight into the project, so it inherits the project quota and
# permissions; applying it to an existing unassigned namespace moves it in.
#
# Rancher splits the project-wide quota between namespaces by *reservation* via
# the field.cattle.io/resourceQuota annotation — a namespace joining the project
# without one only gets whatever is not yet reserved by the others (possibly 0).
# Passing explicit limits pins this namespace's share declaratively, so a full
# redeploy after a cluster wipe reproduces the intended split. The Rancher
# webhook validates that all overrides sum to at most the project quota; if it
# rejects the apply, another namespace's reservation must shrink first (the
# workflows self-heal on the next run of the other deploy).
#
# Without RANCHER_PROJECT_ID the namespace is only created if missing and stays
# outside any project until moved manually (Rancher UI → Cluster →
# Projects/Namespaces).

set -euo pipefail

NS="${1:?namespace required}"
CPU_LIMIT="${2:-}"
MEM_LIMIT="${3:-}"
PROJECT_ID="${RANCHER_PROJECT_ID:-}"

if [[ -n "${PROJECT_ID}" ]]; then
  MANIFEST="apiVersion: v1
kind: Namespace
metadata:
  name: ${NS}
  annotations:
    field.cattle.io/projectId: \"${PROJECT_ID}\""
  if [[ -n "${CPU_LIMIT}" && -n "${MEM_LIMIT}" ]]; then
    MANIFEST+="
    field.cattle.io/resourceQuota: '{\"limit\":{\"limitsCpu\":\"${CPU_LIMIT}\",\"limitsMemory\":\"${MEM_LIMIT}\"}}'"
  fi
  printf '%s\n' "${MANIFEST}" | kubectl apply -f -
else
  if ! kubectl get ns "${NS}" >/dev/null 2>&1; then
    kubectl create namespace "${NS}"
    echo "[namespace] Created ${NS} without a Rancher project (RANCHER_PROJECT_ID not set) — move it into the team project manually so it shares the project quota."
  fi
fi
