#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
git config --local core.hooksPath "$repo_root/git/hooks"

echo "Configured core.hooksPath to $repo_root/git/hooks"
