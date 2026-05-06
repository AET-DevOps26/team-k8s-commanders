#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

echo "==> Setting up Node tools (devDependencies from package.json)"
if command -v npm >/dev/null 2>&1; then
  npm install --no-audit --no-fund
else
  echo "npm not found. Please install Node.js/npm and re-run: https://nodejs.org/"
fi

echo
echo "Installed node binaries:"
ls -1 node_modules/.bin || true
echo
echo "Python venv: .venv/bin"
echo "Done."