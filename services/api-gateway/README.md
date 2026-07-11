# API Gateway

Spring Cloud Gateway — the single public entry point for the CareDesk API. Every
browser/API request hits the gateway on `/api/v1/**`; the gateway verifies the
caller's JWT, translates it into trusted identity headers, and routes the request
to the owning backend service.

This is the only service exposed through the ingress; all backend services sit
behind it on the private cluster network.

## Trusted-header auth model

The gateway is where authentication happens for the whole platform:

1. It **strips** any inbound `X-User-*` headers so a client can't forge an identity.
2. It reads the `Authorization: Bearer <jwt>` header and verifies the token with
   the shared `JWT_SECRET` (the same secret auth-service signs with).
3. On success it injects `X-User-Email`, `X-User-Role` and `X-User-Id` from the
   token claims and forwards the request.

Downstream services do **not** re-verify the JWT; they trust these headers because
only the gateway can reach them (enforced by the cluster NetworkPolicy). See
`JwtAuthenticationFilter`.

## Port & routing

- HTTP port: **8080**
- No database.
- `StripPrefix=2` removes `/api/v1` before forwarding (e.g. `/api/v1/auth/login`
  → `/auth/login` on auth-service).

| Path prefix (`/api/v1/…`) | Routed to | Default URL |
|---|---|---|
| `/auth/**`, `/users/**` | auth-service | `http://localhost:8081` |
| `/ai/**` | ai-assistant | `http://localhost:8000` |
| `/appointments/*/note` | notes-service | `http://localhost:8083` |
| `/notifications/**`, `/appointments/*/notifications` | notification-service | `http://localhost:8084` |
| `/patients/**`, `/doctors/**`, `/appointments/**`, `/clinics/**` | patient-service | `http://localhost:8082` |

Route order matters: the more specific `/appointments/*/note` and
`/appointments/*/notifications` routes are declared before the catch-all
`/appointments/**` patient-service route.

Actuator: `GET /actuator/health`, `GET /actuator/prometheus`.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | _(required)_ | HMAC secret used to verify JWTs. Must match auth-service. |
| `AUTH_SERVICE_URL` | `http://localhost:8081` | auth-service base URL |
| `PATIENT_SERVICE_URL` | `http://localhost:8082` | patient-service base URL |
| `NOTES_SERVICE_URL` | `http://localhost:8083` | notes-service base URL |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8084` | notification-service base URL |
| `AI_ASSISTANT_URL` | `http://localhost:8000` | ai-assistant base URL |
| `CORS_ALLOWED_ORIGINS` | `*` | Allowed browser origins for CORS |
| `APP_VERSION` | `unknown` | Image tag, tagged onto every metric |

## Build, run, test

Java 21 + Maven. Unlike the backend services, the gateway does **not** depend on
the generated OpenAPI stubs.

```bash
cd services/api-gateway
mvn test                       # run tests
JWT_SECRET=dev-secret-32-chars-minimum-length mvn spring-boot:run
```

Or run the whole stack (build context is the repo root):

```bash
docker compose up api-gateway
```
