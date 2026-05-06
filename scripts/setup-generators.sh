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

echo "==> Setting up Python tools (requirements-dev.txt into .venv)"
if command -v python3 >/dev/null 2>&1; then
  if [ ! -d ".venv" ]; then
    python3 -m venv .venv
  fi
  # shellcheck disable=SC1091
  . .venv/bin/activate
  python -m pip install --upgrade pip
  python -m pip install -r requirements-dev.txt
  deactivate
else
  echo "python3 not found. Please install Python 3 and re-run."
fi

echo
echo "Installed node binaries:"
ls -1 node_modules/.bin || true
echo
echo "Python venv: .venv/bin"
echo "Done."