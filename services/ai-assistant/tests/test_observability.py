"""Request correlation and logging tests."""

import logging
from uuid import UUID

from fastapi import FastAPI, Response
from fastapi.testclient import TestClient

from observability import CORRELATION_ID_HEADER, RequestLoggingMiddleware


def _app() -> FastAPI:
    app = FastAPI()
    app.add_middleware(RequestLoggingMiddleware)

    @app.get("/ok")
    async def ok():
        return {"status": "ok"}

    @app.get("/missing")
    async def missing():
        return Response(status_code=404)

    @app.get("/unavailable")
    async def unavailable():
        return Response(status_code=503)

    @app.get("/broken")
    async def broken():
        raise RuntimeError("database password must not appear")

    return app


def test_preserves_correlation_id_and_does_not_log_query(caplog):
    caplog.set_level(logging.INFO, logger="caredesk.request")

    with TestClient(_app()) as client:
        response = client.get(
            "/ok?token=secret",
            headers={CORRELATION_ID_HEADER: "request-123"},
        )

    assert response.status_code == 200
    assert response.headers[CORRELATION_ID_HEADER] == "request-123"
    request_log = next(
        record for record in caplog.records if "request completed" in record.message
    )
    assert request_log.levelno == logging.INFO
    assert "path=/ok" in request_log.message
    assert "token" not in request_log.message
    assert request_log.correlation_id == "request-123"


def test_replaces_unsafe_id_and_logs_client_error(caplog):
    caplog.set_level(logging.WARNING, logger="caredesk.request")

    with TestClient(_app()) as client:
        response = client.get(
            "/missing",
            headers={CORRELATION_ID_HEADER: "unsafe id"},
        )

    UUID(response.headers[CORRELATION_ID_HEADER])
    assert any(record.levelno == logging.WARNING for record in caplog.records)


def test_logs_server_error_response(caplog):
    caplog.set_level(logging.ERROR, logger="caredesk.request")

    with TestClient(_app()) as client:
        response = client.get("/unavailable")

    assert response.status_code == 503
    assert any(record.levelno == logging.ERROR for record in caplog.records)


def test_logs_failure_without_exception_message(caplog):
    caplog.set_level(logging.ERROR, logger="caredesk.request")

    with TestClient(_app(), raise_server_exceptions=False) as client:
        response = client.get(
            "/broken",
            headers={CORRELATION_ID_HEADER: "request-456"},
        )

    assert response.status_code == 500
    request_log = next(
        record for record in caplog.records if "request failed" in record.message
    )
    assert "RuntimeError" in request_log.message
    assert "database password" not in request_log.message
    assert request_log.correlation_id == "request-456"
