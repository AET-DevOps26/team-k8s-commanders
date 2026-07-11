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
echo "==> Checking Java (required by the Spring/FastAPI OpenAPI generators)"
# openapi-generator-cli runs a Java JAR, so gen-all.sh needs a JDK regardless of
# the Node tooling above. We only check here — install a JDK yourself if missing.
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | head -1
  # Pre-fetch the pinned generator JAR (version from openapitools.json) so the
  # first gen-all.sh run doesn't have to download it.
  if [ -x "$repo_root/node_modules/.bin/openapi-generator-cli" ]; then
    "$repo_root/node_modules/.bin/openapi-generator-cli" version >/dev/null 2>&1 || true
  fi
else
  echo "WARNING: java not found. gen-all.sh needs a JDK (11+) for the Spring and"
  echo "FastAPI generators. Install one (e.g. Temurin 21+) before running gen-all.sh."
fi

echo
echo "==> Setting up Python generator tools (pinned, from api/scripts/gen-requirements.txt)"
# gen-all.sh generates the AI assistant's HTTP client with openapi-python-client.
# It self-bootstraps this venv if missing, but we create it here so a fresh
# checkout is fully ready to generate after setup. Kept in a dedicated venv
# (matched by the repo's .venv/ gitignore) so it never pollutes a service venv.
gen_venv="$repo_root/api/scripts/.venv"
if command -v python3 >/dev/null 2>&1; then
  if [ ! -x "$gen_venv/bin/openapi-python-client" ]; then
    python3 -m venv "$gen_venv"
    "$gen_venv/bin/pip" install -q --disable-pip-version-check \
      -r "$repo_root/api/scripts/gen-requirements.txt"
  fi
  echo "Python generator tools: $gen_venv/bin"
else
  echo "python3 not found. Please install Python 3 and re-run: https://www.python.org/"
fi

echo
echo "Done."