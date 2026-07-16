"""Central RFC 9457 exception responses for the AI assistant."""

import logging
from http import HTTPStatus

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

logger = logging.getLogger(__name__)

PROBLEM_JSON = "application/problem+json"


def _title(status_code: int) -> str:
    try:
        return HTTPStatus(status_code).phrase
    except ValueError:
        return "Request Error"


def _problem(
    request: Request,
    status_code: int,
    detail: str,
    *,
    errors: list[dict[str, str]] | None = None,
    headers: dict[str, str] | None = None,
) -> JSONResponse:
    content: dict[str, object] = {
        "type": "about:blank",
        "title": _title(status_code),
        "status": status_code,
        "detail": detail,
        "instance": request.url.path,
    }
    if errors:
        content["errors"] = errors
    return JSONResponse(
        content,
        status_code=status_code,
        headers=headers,
        media_type=PROBLEM_JSON,
    )


async def _http_exception(
    request: Request, exception: StarletteHTTPException
) -> JSONResponse:
    detail = (
        exception.detail
        if isinstance(exception.detail, str)
        else _title(exception.status_code)
    )
    return _problem(
        request,
        exception.status_code,
        detail,
        headers=exception.headers,
    )


async def _validation_exception(
    request: Request, exception: RequestValidationError
) -> JSONResponse:
    errors = [
        {
            "field": ".".join(str(part) for part in error["loc"]),
            "message": error["msg"],
        }
        for error in exception.errors()
    ]
    return _problem(
        request,
        HTTPStatus.BAD_REQUEST,
        "Request validation failed",
        errors=errors,
    )


async def _unexpected_exception(request: Request, exception: Exception) -> JSONResponse:
    logger.error(
        "Unhandled request exception",
        exc_info=(type(exception), exception, exception.__traceback__),
    )
    return _problem(
        request,
        HTTPStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
    )


def install_exception_handlers(app: FastAPI) -> None:
    """Install one response format for HTTP, validation, and unexpected errors."""
    app.add_exception_handler(StarletteHTTPException, _http_exception)
    app.add_exception_handler(RequestValidationError, _validation_exception)
    app.add_exception_handler(Exception, _unexpected_exception)
