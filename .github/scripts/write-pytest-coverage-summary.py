import json
import os
import sys
from pathlib import Path

report = Path(sys.argv[1])
title = sys.argv[2] if len(sys.argv) > 2 else "AI assistant"

try:
    totals = json.loads(report.read_text(encoding="utf-8"))["totals"]
    statements_total = totals["num_statements"]
    statements_covered = totals["covered_lines"]
    branches_total = totals["num_branches"]
    branches_covered = totals["covered_branches"]
    markdown = (
        f"## {title} coverage\n\n"
        "| Metric | Covered | Total | Coverage |\n"
        "|---|---:|---:|---:|\n"
        f"| Statements | {statements_covered} | {statements_total} | "
        f"{totals['percent_statements_covered']:.2f}% |\n"
        f"| Branches | {branches_covered} | {branches_total} | "
        f"{totals['percent_branches_covered']:.2f}% |\n"
        f"| Combined | - | - | {totals['percent_covered']:.2f}% |\n"
    )
except (OSError, KeyError, TypeError, ValueError) as error:
    print(error, file=sys.stderr)
    markdown = f"## {title} coverage\n\nCoverage report was not generated.\n"

summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with Path(summary).open("a", encoding="utf-8") as output:
        output.write(markdown)
else:
    print(markdown, end="")
