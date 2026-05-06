#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"

# Prefer repo-local binaries if available
openapi_gen_bin="$repo_root/node_modules/.bin/openapi-generator-cli"
openapi_ts_bin="$repo_root/node_modules/.bin/openapi-typescript"

if [ -x "$openapi_gen_bin" ]; then
  "$openapi_gen_bin" generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated --skip-validate-spec
else
  openapi-generator-cli generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated --skip-validate-spec
fi

## Generate a FastAPI server stub (for your GenAI FastAPI service)
fastapi_out="services/ai-assistant/generated"
mkdir -p "$fastapi_out"

if [ -x "$openapi_gen_bin" ]; then
  "$openapi_gen_bin" generate -i api/openapi.yaml -g python-fastapi \
    -o "$fastapi_out" --skip-validate-spec
else
  openapi-generator-cli generate -i api/openapi.yaml -g python-fastapi \
    -o "$fastapi_out" --skip-validate-spec
fi

if [ -x "$openapi_ts_bin" ]; then
  "$openapi_ts_bin" api/openapi.yaml -o web-client/src/api.ts
else
  npx openapi-typescript api/openapi.yaml -o web-client/src/api.ts
fi
