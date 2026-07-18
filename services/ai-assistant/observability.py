"""Consistent request logging and correlation for the AI assistant."""

import logging
import os
import re
import time
from contextvars import ContextVar
from uuid import uuid4

from starlette.datastructures import Headers, MutableHeaders

CORRELATION_ID_HEADER = "X-Correlation-ID"
_VALID_CORRELATION_ID = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
_correlation_id: ContextVar[str] = ContextVar("correlation_id", default="none")
logger = logging.getLogger("caredesk.request")


class _CorrelationIdLogFilter(logging.Filter):
    """Adds request correlation to every Python log record."""

    def filter(self, record: logging.LogRecord) -> bool:
        record.correlation_id = _correlation_id.get()
        return True


class _UtcFormatter(logging.Formatter):
    converter = time.gmtime


def configure_logging() -> None:
    """Configure one backend-compatible log format for all Python loggers."""
    requested_level = os.getenv("LOG_LEVEL", "INFO").upper()
    level = getattr(logging, requested_level, logging.INFO)
    handler = logging.StreamHandler()
    handler.addFilter(_CorrelationIdLogFilter())
    handler.setFormatter(
        _UtcFormatter(
            "%(asctime)s level=%(levelname)-5s service=ai-assistant "
            "correlationId=%(correlation_id)s logger=%(name)s message=%(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S%z",
        )
    )

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(level)
    # RequestLoggingMiddleware already emits richer status and duration data.
    logging.getLogger("uvicorn.access").disabled = True


class RequestLoggingMiddleware:
    """ASGI middleware that correlates and logs each HTTP request."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        headers = Headers(scope=scope)
        correlation_id = _normalise_correlation_id(headers.get(CORRELATION_ID_HEADER))
        token = _correlation_id.set(correlation_id)
        started_at = time.perf_counter()
        status_code = 500

        async def correlated_send(message):
            nonlocal status_code
            if message["type"] == "http.response.start":
                status_code = message["status"]
                MutableHeaders(scope=message)[CORRELATION_ID_HEADER] = correlation_id
            await send(message)

        try:
            await self.app(scope, receive, correlated_send)
        except Exception as error:
            logger.error(
                "request failed method=%s path=%s durationMs=%d exception=%s",
                scope["method"],
                scope["path"],
                _elapsed_millis(started_at),
                type(error).__name__,
            )
            raise
        else:
            logger.log(
                _level_for_status(status_code),
                "request completed method=%s path=%s status=%d durationMs=%d",
                scope["method"],
                scope["path"],
                status_code,
                _elapsed_millis(started_at),
            )
        finally:
            _correlation_id.reset(token)


def configure_observability(app) -> None:
    configure_logging()
    app.add_middleware(RequestLoggingMiddleware)


def _normalise_correlation_id(candidate: str | None) -> str:
    if candidate and _VALID_CORRELATION_ID.fullmatch(candidate):
        return candidate
    return str(uuid4())


def _level_for_status(status_code: int) -> int:
    if status_code >= 500:
        return logging.ERROR
    if status_code >= 400:
        return logging.WARNING
    return logging.INFO


def _elapsed_millis(started_at: float) -> int:
    return int((time.perf_counter() - started_at) * 1000)
