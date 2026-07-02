#!/usr/bin/env bash
# Pre-flight check: verify the namespace CPU/memory quota can fit the chart's
# steady-state limits. Fails fast with actionable output instead of a 5–10
# minute Helm timeout.
#
# Usage: check-k8s-quota.sh <namespace> [expected-cpu-m] [expected-mem-mi]
#
# Defaults match helm/caredesk/values.yaml with monitoring enabled:
#   CPU:  6×400m backends + 5×250m Postgres + 1×200m web + 350m monitoring = 4200m
#   Mem:  4096Mi backends + 1280Mi Postgres + 128Mi web + 768Mi monitoring = 6272Mi
# against the team namespace quota of 6000m CPU / 8192Mi memory.

set -euo pipefail

NS="${1:?namespace required}"
EXPECTED_CPU_M="${2:-4200}"
EXPECTED_MEM_MI="${3:-6272}"

cpu_to_m() {
  local value="$1"
  if [[ "${value}" == *m ]]; then
    echo "${value%m}"
  else
    echo $((value * 1000))
  fi
}

mem_to_mi() {
  local value="$1"
  if [[ "${value}" == *Gi ]]; then
    echo $(( ${value%Gi} * 1024 ))
  elif [[ "${value}" == *Mi ]]; then
    echo "${value%Mi}"
  elif [[ "${value}" == *Ki ]]; then
    echo $(( ${value%Ki} / 1024 ))
  else
    # Bare number — Kubernetes treats this as bytes.
    echo $(( value / 1024 / 1024 ))
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

print(hard.get('limits.cpu', ''))
print(used.get('limits.cpu', '0'))
print(hard.get('limits.memory', ''))
print(used.get('limits.memory', '0'))
")"

if [[ "${QUOTA_OUT}" == "__SKIP__" ]]; then
  echo "[quota] No ResourceQuota in namespace ${NS} — skipping check."
  exit 0
fi

CPU_HARD_RAW="$(printf '%s\n' "${QUOTA_OUT}" | sed -n '1p')"
CPU_USED_RAW="$(printf '%s\n' "${QUOTA_OUT}" | sed -n '2p')"
MEM_HARD_RAW="$(printf '%s\n' "${QUOTA_OUT}" | sed -n '3p')"
MEM_USED_RAW="$(printf '%s\n' "${QUOTA_OUT}" | sed -n '4p')"

FAIL=false

if [[ -n "${CPU_HARD_RAW}" ]]; then
  CPU_HARD_M="$(cpu_to_m "${CPU_HARD_RAW}")"
  CPU_USED_M="$(cpu_to_m "${CPU_USED_RAW}")"
  CPU_SPARE_M=$((CPU_HARD_M - CPU_USED_M))

  echo "[quota] Namespace ${NS}: limits.cpu used=${CPU_USED_RAW} hard=${CPU_HARD_RAW} spare=${CPU_SPARE_M}m expected_chart=${EXPECTED_CPU_M}m"

  if (( EXPECTED_CPU_M > CPU_HARD_M )); then
    echo "::error::Chart steady-state CPU (${EXPECTED_CPU_M}m) exceeds namespace hard limit (${CPU_HARD_M}m). Lower limits in helm/caredesk/values.yaml or request a quota increase from AET."
    FAIL=true
  fi

  if (( CPU_USED_M > CPU_HARD_M )); then
    echo "::warning::Namespace already over CPU quota (used=${CPU_USED_M}m > hard=${CPU_HARD_M}m). Clean up stuck ReplicaSets or rollback before redeploying."
  fi
else
  echo "[quota] No limits.cpu hard cap in namespace ${NS} — skipping CPU check."
fi

if [[ -n "${MEM_HARD_RAW}" ]]; then
  MEM_HARD_MI="$(mem_to_mi "${MEM_HARD_RAW}")"
  MEM_USED_MI="$(mem_to_mi "${MEM_USED_RAW}")"
  MEM_SPARE_MI=$((MEM_HARD_MI - MEM_USED_MI))

  echo "[quota] Namespace ${NS}: limits.memory used=${MEM_USED_RAW} hard=${MEM_HARD_RAW} spare=${MEM_SPARE_MI}Mi expected_chart=${EXPECTED_MEM_MI}Mi"

  if (( EXPECTED_MEM_MI > MEM_HARD_MI )); then
    echo "::error::Chart steady-state memory (${EXPECTED_MEM_MI}Mi) exceeds namespace hard limit (${MEM_HARD_MI}Mi). Lower limits in helm/caredesk/values.yaml or request a quota increase from AET."
    FAIL=true
  fi

  if (( MEM_USED_MI > MEM_HARD_MI )); then
    echo "::warning::Namespace already over memory quota (used=${MEM_USED_MI}Mi > hard=${MEM_HARD_MI}Mi). Clean up stuck ReplicaSets or rollback before redeploying."
  fi
else
  echo "[quota] No limits.memory hard cap in namespace ${NS} — skipping memory check."
fi

if [[ "${FAIL}" == "true" ]]; then
  exit 1
fi

echo "[quota] OK — chart budget fits within namespace hard limits."
