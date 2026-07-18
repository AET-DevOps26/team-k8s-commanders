#!/usr/bin/env bash
set -euo pipefail

title=${1:?coverage title required}
report=${2:?JaCoCo CSV path required}
summary=${GITHUB_STEP_SUMMARY:-}

if [[ ! -f "$report" ]]; then
  output="## ${title} coverage\n\nCoverage report was not generated."
else
  table=$(awk -F, '
    NR > 1 {
      instruction_missed += $4; instruction_covered += $5
      branch_missed += $6; branch_covered += $7
      line_missed += $8; line_covered += $9
      method_missed += $12; method_covered += $13
    }
    function row(name, covered, missed) {
      total = covered + missed
      percentage = total ? (100 * covered / total) : 100
      printf "| %s | %d | %d | %.2f%% |\n", name, covered, total, percentage
    }
    END {
      row("Instructions", instruction_covered, instruction_missed)
      row("Branches", branch_covered, branch_missed)
      row("Lines", line_covered, line_missed)
      row("Methods", method_covered, method_missed)
    }
  ' "$report")
  output="## ${title} coverage\n\n| Metric | Covered | Total | Coverage |\n|---|---:|---:|---:|\n${table}"
fi

if [[ -n "$summary" ]]; then
  printf '%b\n' "$output" >> "$summary"
else
  printf '%b\n' "$output"
fi
