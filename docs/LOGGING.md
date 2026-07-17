# Backend logging

All CareDesk backend services write structured, single-line request metadata to
standard output. Container runtimes collect that stream, so Docker Compose and
Kubernetes operators can inspect every service without service-specific log
files or formats.

## Format

Java and Python services share these fields:

```text
2026-07-16T19:30:00+0000 level=INFO service=patient-service correlationId=8d... logger=... message=request completed method=GET path=/appointments status=200 durationMs=12
```

- `service` identifies the emitting backend.
- `correlationId` connects one request across the gateway and downstream calls.
- `logger` identifies the component.
- request completion messages contain method, path, status, and duration.
- query strings, request bodies, authorization headers, and tokens are never logged.

`LOG_LEVEL` changes the root level for every service and defaults to `INFO`.

## Level policy

- `INFO`: successful requests and expected operational milestones.
- `WARN`: rejected or otherwise unsuccessful client requests (`4xx`) and recoverable conditions.
- `ERROR`: server failures (`5xx`), unexpected exceptions, and failed downstream operations.
- `DEBUG`: optional diagnostic detail disabled in normal deployments.

## Request correlation

The API gateway accepts a safe `X-Correlation-ID` or creates a UUID when the
header is absent or invalid. It forwards the selected value and returns it in
the response. Servlet services place it in SLF4J MDC; the AI assistant uses a
request-local context variable. Internal calls from patient-service,
notification-service, and AI assistant propagate the same header.

Correlation ids allow one operation to be found across logs:

```bash
docker compose logs api-gateway patient-service notification-service ai-assistant \
  | rg 'correlationId=8d8a6f89-7bf5-4b35-b8f3-a770e27b7286'
```

Scheduled reminder scans create their own correlation id, which is propagated
to patient-service and cleared after each scan.
