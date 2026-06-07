#!/usr/bin/env bash
# Pre-flight check: verify the namespace CPU quota can fit the chart's steady-state
# CPU limits. Fails fast with actionable output instead of a 5–10 minute Helm timeout.
#
# Usage: check-k8s-quota.sh <namespace> [expected-cpu-limit-millicores]
#
# Default expected budget (2950m) matches helm/caredesk/values.yaml:
#   5 backends × 400m + 3 Postgres × 250m + 1 web × 200m = 2950m

set -euo pipefail

NS="${1:?namespace required}"
EXPECTED_M="${2:-2950}"

cpu_to_m() {
  local value="$1"
  if [[ "${value}" == *m ]]; then
    echo "${value%m}"
  else
    echo $((value * 1000))
  fi
}

RQ_JSON="$(kubectl -n "${NS}" get resourcequota -o json 2>/dev/null || true)"
if [[ -z "${RQ_JSON}" ]]; then
  echo "[quota] No ResourceQuota in namespace ${NS} — skipping check."
  exit 0
fi

QUOTA_OUT="$(echo "${RQ_JSON}" | python3 -c "
import json, sys

data = json.load(sys.stdin)
items = data.get('items') or []
if not items:
    print('__SKIP__')
    sys.exit(0)

status = items[0].get('status') or {}
hard = status.get('hard') or {}
used = status.get('used') or {}
cpu_hard = hard.get('limits.cpu', '')
if not cpu_hard:
    print('__SKIP__')
    sys.exit(0)

print(cpu_hard)
print(used.get('limits.cpu', '0'))
")"

if [[ "${QUOTA_OUT}" == "__SKIP__" ]]; then
  echo "[quota] No ResourceQuota in namespace ${NS} — skipping check."
  exit 0
fi

HARD_RAW="$(printf '%s\n' "${QUOTA_OUT}" | sed -n '1p')"
USED_RAW="$(printf '%s\n' "${QUOTA_OUT}" | sed -n '2p')"

HARD_M="$(cpu_to_m "${HARD_RAW}")"
USED_M="$(cpu_to_m "${USED_RAW}")"
SPARE_M=$((HARD_M - USED_M))

echo "[quota] Namespace ${NS}: limits.cpu used=${USED_RAW} hard=${HARD_RAW} spare=$((SPARE_M))m expected_chart=${EXPECTED_M}m"

if (( EXPECTED_M > HARD_M )); then
  echo "::error::Chart steady-state CPU (${EXPECTED_M}m) exceeds namespace hard limit (${HARD_M}m). Lower limits in helm/caredesk/values.yaml or request a quota increase from AET."
  exit 1
fi

# Warn when already over budget (stale pods from a failed rollout).
if (( USED_M > HARD_M )); then
  echo "::warning::Namespace already over CPU quota (used=${USED_M}m > hard=${HARD_M}m). Clean up stuck ReplicaSets or rollback before redeploying."
fi

echo "[quota] OK — chart budget ${EXPECTED_M}m fits within hard limit ${HARD_M}m."
