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
    -p interfaceOnly=true,useSpringBoot3=true,hideGenerationTimestamp=true
else
  openapi-generator-cli generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated \
    -p interfaceOnly=true,useSpringBoot3=true,hideGenerationTimestamp=true
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
# Model files the AI assistant needs: the session/message request & response
# shapes plus their nested types, and UserRole for DOCTOR/ADMIN authorisation.
# These must exist after generation — fail loudly if the contract drifted.
required_models=(
  ai_session.py
  ai_session_summary.py
  ai_session_create_request.py
  ai_message.py
  ai_message_role.py
  ai_message_request.py
  ai_message_response.py
  paginated_ai_session_response.py
  page_meta.py
  user_role.py
)
if [ -d "$src_models_dir" ]; then
  for model in "${required_models[@]}"; do
    if [ ! -f "$src_models_dir/$model" ]; then
      echo "gen-all.sh: error: expected generated model '$src_models_dir/$model' not found" >&2
      exit 1
    fi
    cp "$src_models_dir/$model" "$target_models_dir"
  done
  # The generator emits cross-model imports rooted at the full `openapi_server`
  # package; the AI service keeps only a flat `models/` package, so rewrite them.
  # Use a backup suffix and delete it so this works with both GNU and BSD sed
  # (BSD/macOS `sed -i` requires an explicit suffix argument).
  for py in "$target_models_dir"/*.py; do
    sed -i.bak 's/from openapi_server\.models\./from models./g' "$py"
    rm -f "$py.bak"
  done
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
