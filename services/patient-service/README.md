# Patient Service

Owns the **scheduling and clinical-workflow** slice of CareDesk: appointments,
doctor availability slots, the patient-facing doctor directory, and clinic
lookups. Patient and doctor *identities* live in auth-service — this service
references them by UUID and stores only the scheduling data keyed on those ids.

Public traffic arrives through the api-gateway, which verifies the JWT and injects
trusted `X-User-*` headers (see the [api-gateway README](../api-gateway/README.md)).

## Responsibilities

- Book, list, reschedule and cancel appointments (per-record ownership enforced
  from the gateway-injected identity headers).
- Manage doctor schedule slots and expose bookable availability.
- Front the doctor directory / specialization list for the booking flow
  (delegating to auth-service, the owner of doctor identity).
- Serve clinic and per-clinic staff lookups.
- Publish an internal feed of soon-due appointments for the reminder scheduler.
- Owns the `appointments` and `doctor_slots` tables in the `patient_db` database.

## Port & data

- HTTP port: **8082**
- Database: `patient_db` (Postgres) — entities `Appointment` → `appointments`,
  `DoctorSlot` → `doctor_slots`.

## Endpoints

Public (via gateway, prefixed `/api/v1`):

- `GET /patients/{patientId}`, `GET /patients/{patientId}/appointments`, `GET /patients/{patientId}/visit-history`
- `GET /doctors`, `GET /doctors/specializations`, `GET /doctors/{doctorId}`
- `GET|POST /doctors/{doctorId}/schedule` — view a doctor's schedule / add an available slot
- `POST|GET /appointments`, `GET|PUT /appointments/{appointmentId}`, `POST /appointments/{appointmentId}/cancel`
- `GET /clinics/{clinicId}`, `GET /clinics/{clinicId}/staff`

Internal (pod-to-pod only; **not** routed by the gateway, guarded by the
NetworkPolicy and permitted anonymously in `SecurityConfig`):

- `GET /internal/appointments/upcoming` — active appointments due within N hours,
  polled by notification-service's reminder scheduler.

Actuator: `GET /actuator/health`, `GET /actuator/prometheus`.

The full request/response contracts live in [`api/openapi.yaml`](../../api/openapi.yaml).

## Cross-service calls

- **auth-service** (`AUTH_SERVICE_URL`) — resolve doctor/patient profiles and the
  specialization list.
- **notification-service** (`NOTIFICATION_SERVICE_URL`) — best-effort
  booking-confirmation triggers.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `patient_db` / `caredesk` / `caredesk` | Postgres connection |
| `AUTH_SERVICE_URL` | `http://localhost:8081` | auth-service base URL |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8084` | notification-service base URL |
| `SPRING_PROFILES_ACTIVE` | _(none)_ | Set to `dev` to enable demo-data seeding |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | `update` in compose to create the schema on a fresh DB |
| `APP_VERSION` | `unknown` | Image tag, tagged onto every metric |

## Build, run, test

Java 21 + Maven. This service compiles against OpenAPI stubs generated from
`api/openapi.yaml` — generate and install them into the local Maven repo first:

```bash
./api/scripts/gen-all.sh
(cd services/springboot/generated && mvn -q install)
```

Then:

```bash
cd services/patient-service
mvn test                       # run tests
mvn spring-boot:run            # run locally (needs a reachable Postgres + auth-service)
```

Or run the whole stack (build context is the repo root):

```bash
docker compose up patient-service
```
