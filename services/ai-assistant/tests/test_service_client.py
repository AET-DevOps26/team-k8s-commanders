"""Unit tests for the backend HTTP helpers, focused on status handling.

These exercise the real ``_get_json`` path (which ``test_context`` bypasses by
mocking ``service_client``) by routing requests through an ``httpx.MockTransport``.
"""

import asyncio
import functools
from unittest.mock import patch

import httpx
import pytest

from utils import service_client


def _run_with_status(status_code: int, json_body: dict | None = None):
    """Drive ``get_appointment_note`` against a transport returning ``status_code``."""

    def handler(_request: httpx.Request) -> httpx.Response:
        return httpx.Response(status_code, json=json_body)

    transport = httpx.MockTransport(handler)
    # ``_get_json`` constructs its own AsyncClient; inject the mock transport so
    # the request is served locally without a real socket.
    client_factory = functools.partial(httpx.AsyncClient, transport=transport)
    with patch.object(service_client.httpx, "AsyncClient", client_factory):
        return asyncio.run(service_client.get_appointment_note("a-1", headers={}))


def test_get_json_returns_none_on_not_found():
    assert _run_with_status(httpx.codes.NOT_FOUND) is None


def test_get_json_returns_none_on_forbidden():
    # A note authored by a different doctor comes back 403; it must be skipped,
    # not raised, so a multi-doctor visit history doesn't fail the whole query.
    assert _run_with_status(httpx.codes.FORBIDDEN) is None


def test_get_json_returns_body_on_success():
    assert _run_with_status(httpx.codes.OK, {"content": "All good"}) == {"content": "All good"}


def test_get_json_raises_on_server_error():
    with pytest.raises(httpx.HTTPStatusError):
        _run_with_status(httpx.codes.INTERNAL_SERVER_ERROR)
