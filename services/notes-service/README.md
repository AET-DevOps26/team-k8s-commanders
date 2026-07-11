# Notes Service

Owns **clinical notes**: the diagnosis and free-text record a doctor attaches to a
completed appointment. Each note is keyed on an appointment id (owned by
patient-service) and a doctor id (owned by auth-service); this service stores only
the note content and diagnosis.

Public traffic arrives through the api-gateway, which verifies the JWT and injects
trusted `X-User-*` headers (see the [api-gateway README](../api-gateway/README.md)).
The gateway routes the note sub-path (`/appointments/*/note`) here specifically,
ahead of the general appointments route that goes to patient-service.

## Responsibilities

- Create / read the clinical note for an appointment (one note per appointment).
- Provide grounded clinical content that the AI assistant summarises.
- Owns the `clinical_notes` table in the `notes_db` database.

## Port & data

- HTTP port: **8083**
- Database: `notes_db` (Postgres) — entity `ClinicalNote` → `clinical_notes` table.

## Endpoints

Public (via gateway, prefixed `/api/v1`):

- `GET|PUT|DELETE /appointments/{appointmentId}/note` — read / upsert / delete the clinical note

Actuator: `GET /actuator/health`, `GET /actuator/prometheus`.

The full request/response contracts live in [`api/openapi.yaml`](../../api/openapi.yaml).

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `notes_db` / `caredesk` / `caredesk` | Postgres connection |
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
cd services/notes-service
mvn test                       # run tests
mvn spring-boot:run            # run locally (needs a reachable Postgres)
```

Or run the whole stack (build context is the repo root):

```bash
docker compose up notes-service
```
