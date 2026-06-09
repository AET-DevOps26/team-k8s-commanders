#!/usr/bin/env bash
#
# demo-ai-context.sh — end-to-end smoke test of the AI assistant's grounding.
#
# Against a locally running docker-compose stack this script:
#   1. logs in as the seeded DOCTOR account,
#   2. registers (or reuses) a PATIENT,
#   3. books two appointments for that patient,
#   4. writes a clinical note (with diagnosis) for each appointment,
#   5. asks the AI assistant — as the doctor — about the patient, both by
#      patient_id (should pull in every appointment + note) and by a single
#      appointment_id.
#
# The point is to eyeball the `sources` array and the answer to confirm the
# assistant actually received the appointment and note context.
#
# Prerequisites: the stack must be up (`docker compose up -d`) and reachable at
# the gateway. Requires curl + jq.
#
# Usage:
#   ./scripts/demo-ai-context.sh
#   BASE_URL=http://localhost:8080/api/v1 ./scripts/demo-ai-context.sh
set -euo pipefail

# ── Config ───────────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"

# Seeded by auth-service DefaultUserSeeder.
DOCTOR_EMAIL="${DOCTOR_EMAIL:-doctor@doctor.com}"
DOCTOR_PASSWORD="${DOCTOR_PASSWORD:-doctor123}"

# The patient we ground answers on. Reused across runs (409 on register → login).
PATIENT_NAME="${PATIENT_NAME:-Maria Schmidt}"
PATIENT_EMAIL="${PATIENT_EMAIL:-maria.schmidt@example.com}"
PATIENT_PASSWORD="${PATIENT_PASSWORD:-patient123}"

# ── Pretty output ────────────────────────────────────────────────────────────
if [ -t 1 ]; then BOLD=$'\033[1m'; DIM=$'\033[2m'; GREEN=$'\033[32m'; CYAN=$'\033[36m'; RED=$'\033[31m'; RESET=$'\033[0m'
else BOLD=""; DIM=""; GREEN=""; CYAN=""; RED=""; RESET=""; fi
step() { printf '\n%s==> %s%s\n' "$BOLD$CYAN" "$1" "$RESET"; }
info() { printf '    %s%s%s\n' "$DIM" "$1" "$RESET"; }
die()  { printf '%sERROR: %s%s\n' "$RED" "$1" "$RESET" >&2; exit 1; }

command -v jq >/dev/null   || die "jq is required"
command -v curl >/dev/null || die "curl is required"

# api METHOD PATH [JSON_BODY] [BEARER_TOKEN]
# Performs the request, splits body from the trailing HTTP status, and fails
# loudly (printing the body) on any non-2xx response.
api() {
  local method="$1" path="$2" body="${3:-}" token="${4:-}"
  local args=(-sS -X "$method" "${BASE_URL}${path}" -w $'\n%{http_code}')
  [ -n "$body" ]  && args+=(-H "Content-Type: application/json" -d "$body")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer ${token}")

  local response status payload
  response="$(curl "${args[@]}")" || die "curl failed: $method $path"
  status="${response##*$'\n'}"
  payload="${response%$'\n'*}"

  if [ "$status" -lt 200 ] || [ "$status" -ge 300 ]; then
    die "$method $path -> HTTP $status"$'\n'"$payload"
  fi
  printf '%s' "$payload"
}

# ── 0. Wait for the gateway ──────────────────────────────────────────────────
step "Waiting for the gateway at ${BASE_URL}"
for i in $(seq 1 30); do
  # /auth/login is public; a 4xx still proves the gateway + auth-service are up.
  if curl -sS -o /dev/null "${BASE_URL}/auth/login" -X POST \
       -H "Content-Type: application/json" -d '{}' 2>/dev/null; then
    info "gateway is responding"; break
  fi
  [ "$i" -eq 30 ] && die "gateway not reachable — is the stack up? (docker compose up -d)"
  sleep 2
done

# ── 1. Doctor login ──────────────────────────────────────────────────────────
step "Logging in as doctor (${DOCTOR_EMAIL})"
doctor_session="$(api POST /auth/login \
  "$(jq -nc --arg e "$DOCTOR_EMAIL" --arg p "$DOCTOR_PASSWORD" '{email:$e,password:$p}')")"
DOCTOR_TOKEN="$(jq -r '.accessToken' <<<"$doctor_session")"
DOCTOR_ID="$(jq -r '.user.id' <<<"$doctor_session")"
[ "$DOCTOR_TOKEN" != "null" ] || die "no accessToken in doctor login response"
info "doctor id: $DOCTOR_ID"

# ── 2. Patient (register, or reuse if already present) ───────────────────────
step "Registering patient (${PATIENT_EMAIL})"
register_body="$(jq -nc --arg n "$PATIENT_NAME" --arg e "$PATIENT_EMAIL" --arg p "$PATIENT_PASSWORD" \
  '{name:$n,email:$e,password:$p}')"
# Register is best-effort: a fresh run creates the patient; a repeat run fails
# because the email already exists (auth-service currently returns 500 for that,
# not 409). Either way we then log in with the known password — login is the
# source of truth for the patient id, so we don't depend on the register status.
reg_status="$(curl -sS -o /dev/null -w '%{http_code}' -X POST "${BASE_URL}/auth/register" \
  -H "Content-Type: application/json" -d "$register_body")"
if [ "$reg_status" -ge 200 ] && [ "$reg_status" -lt 300 ]; then
  info "patient registered"
else
  info "patient not newly created (register -> HTTP $reg_status) — assuming it already exists, logging in"
fi
patient_session="$(api POST /auth/login \
  "$(jq -nc --arg e "$PATIENT_EMAIL" --arg p "$PATIENT_PASSWORD" '{email:$e,password:$p}')")"
PATIENT_ID="$(jq -r '.user.id' <<<"$patient_session")"
[ "$PATIENT_ID" != "null" ] || die "could not resolve patient id (register HTTP $reg_status, login returned no user)"
info "patient id: $PATIENT_ID"

# ── 3. Book two appointments (as the doctor) ─────────────────────────────────
# The backend only accepts a booking that lands exactly on an *available*
# DoctorSlot, and slots are published for a fixed doctor by the dev data seeder
# (not for the freshly-registered login account). So we book against that seeded
# doctor and, rather than guessing times, read its schedule and book whatever
# slots are actually free — which keeps the demo working no matter when it runs.
SEED_DOCTOR_ID="${SEED_DOCTOR_ID:-22222222-2222-2222-2222-222222222222}"

# book_appointment DATETIME DURATION REASON DOCTOR_ID  -> echoes the new appointment id
book_appointment() {
  local dt="$1" dur="$2" reason="$3" did="$4" appt
  appt="$(api POST /appointments \
    "$(jq -nc --arg pid "$PATIENT_ID" --arg did "$did" --arg dt "$dt" \
              --argjson dur "$dur" --arg r "$reason" \
       '{patientId:$pid,doctorId:$did,dateTime:$dt,duration:$dur,reason:$r}')" \
    "$DOCTOR_TOKEN")"
  jq -r '.id' <<<"$appt"
}

step "Finding two open slots in the doctor's schedule"
schedule="$(api GET "/doctors/${SEED_DOCTOR_ID}/schedule" "" "$DOCTOR_TOKEN")"
# Earliest two available, *future* slots (the backend rejects past bookings),
# each as "startAt durationMinutes".
mapfile -t SLOTS < <(jq -r '
  [.slots[] | select(.available and ((.startAt|fromdateiso8601) > now))]
  | sort_by(.startAt) | .[0:2][]
  | "\(.startAt) \(((( .endAt|fromdateiso8601) - (.startAt|fromdateiso8601)) / 60) | floor)"' <<<"$schedule")
[ "${#SLOTS[@]}" -ge 2 ] \
  || die "doctor ${SEED_DOCTOR_ID} has fewer than 2 open future slots (found ${#SLOTS[@]}). Restart patient-service to reseed its schedule."
info "using slots: ${SLOTS[0]} | ${SLOTS[1]}"

step "Booking two appointments for the patient"
APPT1_ID="$(book_appointment "${SLOTS[0]% *}" "${SLOTS[0]#* }" "Annual check-up, persistent cough" "$SEED_DOCTOR_ID")"
info "appointment #1: $APPT1_ID  (${SLOTS[0]% *}, persistent cough)"
APPT2_ID="$(book_appointment "${SLOTS[1]% *}" "${SLOTS[1]#* }" "Follow-up on blood pressure" "$SEED_DOCTOR_ID")"
info "appointment #2: $APPT2_ID  (${SLOTS[1]% *}, blood pressure follow-up)"

# ── 4. Write a clinical note per appointment (as the doctor) ──────────────────
# upsert_note APPOINTMENT_ID CONTENT DIAG_CODE DIAG_DESC
upsert_note() {
  api PUT "/appointments/$1/note" \
    "$(jq -nc --arg c "$2" --arg code "$3" --arg desc "$4" \
       '{content:$c, diagnosis:{code:$code, description:$desc}}')" \
    "$DOCTOR_TOKEN" >/dev/null
}

step "Writing a clinical note for each appointment"
upsert_note "$APPT1_ID" \
  "Patient reports a dry cough lasting three weeks. Lungs clear on auscultation. Prescribed rest and fluids; advised to return if symptoms persist." \
  "R05" "Cough"
info "note saved for appointment #1 (R05 Cough)"
upsert_note "$APPT2_ID" \
  "Blood pressure 150/95, elevated from last visit. Started on low-dose amlodipine. Advised reduced salt intake and follow-up in one month." \
  "I10" "Essential (primary) hypertension"
info "note saved for appointment #2 (I10 Hypertension)"

# ── 5. Ask the AI assistant, as the doctor ───────────────────────────────────
# Conversations are persistent now: open a session (optionally bound to a
# patient/appointment) and post messages to it. The binding grounds every turn.
#
# new_session JSON_BODY -> prints the new session id
new_session() {
  api POST /ai/sessions "$1" "$DOCTOR_TOKEN" | jq -r '.id'
}

# ask SESSION_ID QUERY -> prints answer + sources (single response, waits for the end)
ask() {
  local resp
  resp="$(api POST "/ai/sessions/$1/messages" \
    "$(jq -nc --arg q "$2" '{query:$q}')" "$DOCTOR_TOKEN")"
  printf '%s    sources:%s %s\n' "$BOLD" "$RESET" "$(jq -c '.sources // []' <<<"$resp")"
  printf '%s    answer:%s\n'      "$BOLD" "$RESET"
  jq -r '.answer' <<<"$resp" | sed 's/^/      /'
}

# ask_stream SESSION_ID QUERY -> prints sources, then the answer token-by-token as
# it arrives. Sends `Accept: text/event-stream`; `curl -N` keeps the stream
# unbuffered so the tokens appear live instead of all at once.
ask_stream() {
  curl -sS -N -X POST "${BASE_URL}/ai/sessions/$1/messages" \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream" \
    -H "Authorization: Bearer ${DOCTOR_TOKEN}" \
    -d "$(jq -nc --arg q "$2" '{query:$q}')" \
  | { event=""
      while IFS= read -r line; do
        case "$line" in
          "event: "*) event="${line#event: }" ;;
          "data: "*)
            data="${line#data: }"
            case "$event" in
              sources) printf '%s    sources:%s %s\n%s    answer (streaming):%s\n      ' \
                         "$BOLD" "$RESET" "$(jq -c . <<<"$data")" "$BOLD" "$RESET" ;;
              token)   printf '%s' "$(jq -r . <<<"$data")" ;;
              error)   printf '\n%s    stream error: %s%s\n' "$RED" "$(jq -r .detail <<<"$data")" "$RESET" ;;
              done)    printf '\n' ;;
            esac ;;
        esac
      done; }
}

step "AI session #1 — bound to patient_id, STREAMED (expects profile + BOTH appointments + BOTH notes)"
PATIENT_SESSION="$(new_session "$(jq -nc --arg pid "$PATIENT_ID" '{patientId:$pid}')")"
info "session id: $PATIENT_SESSION"
ask_stream "$PATIENT_SESSION" \
  "Summarise this patient's visit history and list every diagnosis on record."

step "Follow-up in the SAME session — proves the conversation has memory"
ask "$PATIENT_SESSION" \
  "Of those diagnoses, which one needs follow-up soonest and why?"

step "AI session #2 — bound to appointment_id, single JSON response (single appointment + its note)"
APPT_SESSION="$(new_session "$(jq -nc --arg aid "$APPT2_ID" '{appointmentId:$aid}')")"
info "session id: $APPT_SESSION"
ask "$APPT_SESSION" "What happened at this appointment and what was prescribed?"

# ── 6. Clean up so the demo is re-runnable ───────────────────────────────────
# Cancelling releases the doctor slots we consumed, so the next run finds them
# free again instead of failing with "slot unavailable".
step "Cleaning up (cancelling the demo appointments to free the slots)"
api POST "/appointments/${APPT1_ID}/cancel" "" "$DOCTOR_TOKEN" >/dev/null && info "cancelled appointment #1"
api POST "/appointments/${APPT2_ID}/cancel" "" "$DOCTOR_TOKEN" >/dev/null && info "cancelled appointment #2"

printf '\n%s✓ Done.%s If the sources include "Clinical note" and "Appointment record" and the\n' "$GREEN" "$RESET"
printf '  answer mentions the cough and the hypertension, the grounding context is working.\n'
printf '  Session #1 streamed token-by-token (SSE), then a follow-up reused the conversation;\n'
printf '  session #2 used the buffered JSON response.\n'
