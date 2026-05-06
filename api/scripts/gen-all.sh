#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"

# Prefer repo-local binaries if available
openapi_gen_bin="$repo_root/node_modules/.bin/openapi-generator-cli"
openapi_py_bin="$repo_root/.venv/bin/openapi-python-client"
openapi_ts_bin="$repo_root/node_modules/.bin/openapi-typescript"

if [ -x "$openapi_gen_bin" ]; then
  "$openapi_gen_bin" generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated --skip-validate-spec
else
  openapi-generator-cli generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated --skip-validate-spec
fi

py_out="services/ai-assitant/client"
mkdir -p "$(dirname "$py_out")"

# Ensure .venv/bin is on PATH so tools like `ruff` are available to the Python client
export PATH="$repo_root/.venv/bin:$PATH"

if [ -x "$openapi_py_bin" ]; then
  py_config="$repo_root/api/scripts/py-config.json"
  if [ -f "$py_config" ]; then
    "$openapi_py_bin" generate --path api/openapi.yaml \
      --output-path "$py_out" --config "$py_config" --overwrite
  else
    "$openapi_py_bin" generate --path api/openapi.yaml \
      --output-path "$py_out" --overwrite
  fi
else
  py_config="api/scripts/py-config.json"
  if [ -f "$py_config" ]; then
    openapi-python-client generate --path api/openapi.yaml \
      --output-path "$py_out" --config "$py_config" --overwrite
  else
    openapi-python-client generate --path api/openapi.yaml \
      --output-path "$py_out" --overwrite
  fi
fi

if [ -x "$openapi_ts_bin" ]; then
  "$openapi_ts_bin" api/openapi.yaml -o web-client/src/api.ts
else
  npx openapi-typescript api/openapi.yaml -o web-client/src/api.ts
fi
