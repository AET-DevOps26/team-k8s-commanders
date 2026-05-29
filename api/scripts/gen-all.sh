#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"

# Prefer repo-local binaries if available
openapi_gen_bin="$repo_root/node_modules/.bin/openapi-generator-cli"
openapi_ts_bin="$repo_root/node_modules/.bin/openapi-typescript"

spring_gen_dir="$repo_root/services/springboot/generated"
rm -rf "$spring_gen_dir/src" "$spring_gen_dir/target"

if [ -x "$openapi_gen_bin" ]; then
  "$openapi_gen_bin" generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated \
    -p interfaceOnly=true,useSpringBoot3=true
else
  openapi-generator-cli generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated \
    -p interfaceOnly=true,useSpringBoot3=true
fi

## Generate only the FastAPI model objects needed by the AI query endpoint.
# Generate into a temporary folder then copy only the selected model files to the target.
fastapi_temp_dir=$(mktemp -d)
if [ -x "$openapi_gen_bin" ]; then
  "$openapi_gen_bin" generate -i api/openapi.yaml -g python-fastapi \
    -o "$fastapi_temp_dir" \
    --global-property models,apis=false,supportingFiles=false,apiDocs=false,apiTests=false,modelDocs=false,modelTests=false
else
  openapi-generator-cli generate -i api/openapi.yaml -g python-fastapi \
    -o "$fastapi_temp_dir" \
    --global-property models,apis=false,supportingFiles=false,apiDocs=false,apiTests=false,modelDocs=false,modelTests=false
fi

src_models_dir="$fastapi_temp_dir/src/openapi_server/models"
target_models_dir="$repo_root/services/ai-assistant/models"
rm -rf "$target_models_dir"
mkdir -p "$target_models_dir"
if [ -d "$src_models_dir" ]; then
  # copy only the model files needed by the AI query endpoint
  cp "$src_models_dir/ai_query_request.py" "$target_models_dir" 2>/dev/null || true
  cp "$src_models_dir/ai_query_response.py" "$target_models_dir" 2>/dev/null || true
  # UserRole is used to authorise /ai/query (DOCTOR/ADMIN only)
  cp "$src_models_dir/user_role.py" "$target_models_dir" 2>/dev/null || true
fi
# ensure package init
if [ ! -f "$target_models_dir/__init__.py" ]; then
  touch "$target_models_dir/__init__.py"
fi
# cleanup temp generation folder
rm -rf "$fastapi_temp_dir"

if [ -x "$openapi_ts_bin" ]; then
  "$openapi_ts_bin" api/openapi.yaml -o web-client/src/api.ts
else
  npx openapi-typescript api/openapi.yaml -o web-client/src/api.ts
fi
