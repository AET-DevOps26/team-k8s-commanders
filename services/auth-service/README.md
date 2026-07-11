# Auth Service

Owns **user identity** for CareDesk: registration, login (JWT issuance), account
profiles, and the doctor directory. It is the single source of truth for who
exists — patients, doctors and the administrator — and every other service
references users by the UUIDs minted here.

Public traffic arrives through the api-gateway, which verifies the JWT and injects
trusted `X-User-*` headers (see the [api-gateway README](../api-gateway/README.md)).
auth-service is also the service that **signs** those JWTs at login.

## Responsibilities

- Register patients and authenticate all users, issuing HS256 JWTs (`JWT_SECRET`,
  24h expiry) whose claims carry the user's email, role and id.
- Store and update account profiles (name, contact, role-specific fields such as a
  doctor's specialization / license / clinic).
- Serve the internal doctor directory (search + distinct specializations) consumed
  by patient-service's booking flow.
- Bootstrap the administrator from deployment env vars, and (in the `dev` profile)
  seed demo users.
- Owns the `users` table in the `auth_db` database.

## Port & data

- HTTP port: **8081**
- Database: `auth_db` (Postgres) — entity `User` → `users` table.

## Endpoints

Public (via gateway, prefixed `/api/v1`):

- `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`
- `GET /users`, `GET /users/stats`, `GET /users/{userId}`, `PUT /users/{userId}/password`

Internal (pod-to-pod only; **not** routed by the gateway, guarded by the
NetworkPolicy and permitted anonymously in `SecurityConfig`):

- `GET /internal/doctors` — doctor search (name / specialization, paginated)
- `GET /internal/doctors/specializations` — distinct specializations of enabled doctors

Actuator: `GET /actuator/health`, `GET /actuator/prometheus`.

The full request/response contracts live in [`api/openapi.yaml`](../../api/openapi.yaml).

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | _(required)_ | HMAC secret for signing JWTs. Must match the gateway. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `auth_db` / `caredesk` / `caredesk` | Postgres connection |
| `CAREDESK_BOOTSTRAP_ADMIN_ENABLED` | `false` | Create the admin from the vars below on startup |
| `CAREDESK_ADMIN_NAME` / `CAREDESK_ADMIN_EMAIL` / `CAREDESK_ADMIN_PASSWORD` | _(empty)_ | Bootstrap admin credentials |
| `SPRING_PROFILES_ACTIVE` | _(none)_ | Set to `dev` to enable demo-user seeding |
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
cd services/auth-service
mvn test                       # run tests
mvn spring-boot:run            # run locally (needs a reachable Postgres)
```

Or run the whole stack (build context is the repo root):

```bash
docker compose up auth-service
```
