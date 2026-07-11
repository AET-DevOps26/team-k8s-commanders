# Notification Service

Owns **notifications**: the record of messages sent to users (booking
confirmations, reschedule/cancel notices, appointment reminders) and their SMTP
delivery. It receives triggers from patient-service, persists a notification row,
and delivers the mail; it also runs a scheduler that reminds patients of soon-due
appointments.

Public traffic arrives through the api-gateway, which verifies the JWT and injects
trusted `X-User-*` headers (see the [api-gateway README](../api-gateway/README.md)).

## Responsibilities

- Persist a notification per user-facing event and deliver it over SMTP (Mailpit
  in dev; any SMTP provider via config).
- Expose a user's notification history / per-appointment notifications.
- Run the **reminder scheduler**: periodically poll patient-service's internal
  upcoming-appointments feed and send reminders for appointments due within the
  configured window.
- Owns the `notifications` table in the `notification_db` database.

## Port & data

- HTTP port: **8084**
- Database: `notification_db` (Postgres) — entity `Notification` → `notifications` table.

## Endpoints

Public (via gateway, prefixed `/api/v1`):

- `GET /notifications`, `GET /notifications/{notificationId}` — list / read a notification
- `GET /appointments/{appointmentId}/notifications` — notifications for an appointment

Internal (pod-to-pod only; **not** routed by the gateway, guarded by the
NetworkPolicy and permitted anonymously in `SecurityConfig`):

- `POST /internal/notifications` — trigger a notification (called by patient-service)

Actuator: `GET /actuator/health`, `GET /actuator/prometheus`.

The full request/response contracts live in [`api/openapi.yaml`](../../api/openapi.yaml).

## Cross-service calls

- **patient-service** (`PATIENT_SERVICE_URL`) — the reminder scheduler polls its
  internal `GET /internal/appointments/upcoming` feed.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `notification_db` / `caredesk` / `caredesk` | Postgres connection |
| `PATIENT_SERVICE_URL` | `http://localhost:8082` | patient-service base URL (reminder feed) |
| `SMTP_HOST` / `SMTP_PORT` | `localhost` / `1025` | SMTP target (defaults to local Mailpit) |
| `SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_AUTH` / `SMTP_STARTTLS` | empty / empty / `false` / `false` | SMTP auth/TLS for a real provider |
| `MAIL_FROM` | `CareDesk <no-reply@caredesk.local>` | From address on outgoing mail |
| `REMINDER_ENABLED` | `true` | Toggle the reminder scheduler (set `false` in tests) |
| `REMINDER_WINDOW_HOURS` | `24` | How far ahead to look for reminders |
| `REMINDER_SCAN_INTERVAL_MS` / `REMINDER_INITIAL_DELAY_MS` | `900000` / `60000` | Scan cadence / startup delay |
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
cd services/notification-service
mvn test                       # run tests (reminder scheduler disabled)
mvn spring-boot:run            # run locally (needs a reachable Postgres + SMTP)
```

Or run the whole stack, which includes Mailpit as the SMTP catch-all (build
context is the repo root):

```bash
docker compose up notification-service
```
