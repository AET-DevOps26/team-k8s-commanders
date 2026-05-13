#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

echo "==> Running generator setup"
./scripts/setup-generators.sh

echo "==> Installing git hooks (repo-managed)"
./scripts/install-hooks.sh

echo "==> Attempting initial code generation (may fail if generators missing)"
if ./api/scripts/gen-all.sh; then
  echo "Generation completed."
else
  echo "Generation failed or generators missing—check scripts/setup-generators.sh output."
fi

echo "Setup complete."
