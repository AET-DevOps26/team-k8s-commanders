#!/usr/bin/env bash
# Ensure a namespace exists and belongs to the team's Rancher project.
#
# Usage: RANCHER_PROJECT_ID=<cluster-id>:<project-id> ensure-k8s-namespace.sh <namespace>
#
# Rancher assigns namespaces to projects via the field.cattle.io/projectId
# annotation (e.g. "c-m-abc12def:p-xyz45" — both IDs are visible in the Rancher
# URL while the project is open). Applying the annotation at creation time puts
# the namespace straight into the project, so it inherits the project quota and
# permissions; applying it to an existing unassigned namespace moves it in.
#
# Without RANCHER_PROJECT_ID the namespace is only created if missing and stays
# outside any project until moved manually (Rancher UI → Cluster →
# Projects/Namespaces).

set -euo pipefail

NS="${1:?namespace required}"
PROJECT_ID="${RANCHER_PROJECT_ID:-}"

if [[ -n "${PROJECT_ID}" ]]; then
  kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: ${NS}
  annotations:
    field.cattle.io/projectId: "${PROJECT_ID}"
EOF
else
  if ! kubectl get ns "${NS}" >/dev/null 2>&1; then
    kubectl create namespace "${NS}"
    echo "[namespace] Created ${NS} without a Rancher project (RANCHER_PROJECT_ID not set) — move it into the team project manually so it shares the project quota."
  fi
fi
