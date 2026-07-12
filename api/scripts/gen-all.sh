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
    -p interfaceOnly=true,useSpringBoot4=true,hideGenerationTimestamp=true,openApiNullable=false
else
  openapi-generator-cli generate -i api/openapi.yaml -g spring \
    -o services/springboot/generated \
    -p interfaceOnly=true,useSpringBoot4=true,hideGenerationTimestamp=true,openApiNullable=false
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

## Generate the Python HTTP client the AI assistant uses for its downstream
# calls (patient-service, notes-service). openapi-python-client emits an
# httpx + attrs client; we vendor only the four endpoints the assistant calls,
# plus their transitive model closure and the small runtime — mirroring the
# selective FastAPI-model copy above. The tool is pinned in a dedicated venv so
# the output is reproducible.
opc_venv="$repo_root/api/scripts/.venv"
if [ ! -x "$opc_venv/bin/openapi-python-client" ]; then
  python3 -m venv "$opc_venv"
  "$opc_venv/bin/pip" install -q --disable-pip-version-check \
    -r "$repo_root/api/scripts/gen-requirements.txt"
fi

client_temp_dir=$(mktemp -d)
# Put the venv bin on PATH so the generator's ruff formatting/import-pruning runs.
PATH="$opc_venv/bin:$PATH" "$opc_venv/bin/openapi-python-client" generate \
  --path api/openapi.yaml \
  --output-path "$client_temp_dir/caredesk_client" \
  --meta none

client_src="$client_temp_dir/caredesk_client"
client_dst="$repo_root/services/ai-assistant/caredesk_client"
rm -rf "$client_dst"
mkdir -p "$client_dst/api/patients" "$client_dst/api/appointments" "$client_dst/models"

# Runtime + package/endpoint __init__ files (api/*/__init__.py are just docstrings).
required_client_files=(
  __init__.py
  client.py
  errors.py
  types.py
  api/__init__.py
  api/patients/__init__.py
  api/appointments/__init__.py
  # The four endpoints the assistant calls ...
  api/patients/get_patient_by_id.py
  api/patients/get_patient_visit_history.py
  api/appointments/get_appointment_by_id.py
  api/appointments/get_appointment_note.py
  # ... and the transitive model closure they reference.
  models/user_profile.py
  models/user_role.py
  models/visit_history.py
  models/appointment.py
  models/appointment_status.py
  models/clinical_note.py
  models/diagnosis.py
  models/problem_detail.py
  models/validation_error.py
)
for f in "${required_client_files[@]}"; do
  if [ ! -f "$client_src/$f" ]; then
    echo "gen-all.sh: error: expected generated client file '$f' not found" >&2
    exit 1
  fi
  cp "$client_src/$f" "$client_dst/$f"
done
# Endpoints import model submodules directly, so an empty models package init is
# enough (matches the flat FastAPI models/ package above).
: > "$client_dst/models/__init__.py"
rm -rf "$client_temp_dir"

# Smoke-test the vendored subset: catches a missing transitive model the
# hand-maintained closure above would otherwise miss. Runs under the tool venv,
# which has the client's only runtime deps (httpx + attrs).
if ! "$opc_venv/bin/python" -c "
import sys
sys.path.insert(0, '$repo_root/services/ai-assistant')
import caredesk_client
from caredesk_client.api.patients import get_patient_by_id, get_patient_visit_history
from caredesk_client.api.appointments import get_appointment_by_id, get_appointment_note
"; then
  echo "gen-all.sh: error: vendored caredesk_client failed its import smoke-test" >&2
  exit 1
fi

if [ -x "$openapi_ts_bin" ]; then
  "$openapi_ts_bin" api/openapi.yaml -o web-client/src/api.ts
else
  npx openapi-typescript api/openapi.yaml -o web-client/src/api.ts
fi
