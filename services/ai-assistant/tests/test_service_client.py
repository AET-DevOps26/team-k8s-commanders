"""Unit tests for the backend service clients, focused on status handling.

These drive the real generated ``caredesk_client`` (parsing included) by
injecting an ``httpx.MockTransport`` into its ``Client``, so they exercise the
vendored client and the status adapter together — ``test_context`` mocks
``service_client`` entirely and covers none of this.
"""

import asyncio

import httpx
import pytest

from caredesk_client import Client
from utils import service_client

_NO_JSON = object()

# A spec-valid ClinicalNote: the generated model's from_dict requires these
# fields (UUIDs + an ISO timestamp), unlike the old dict passthrough.
_VALID_NOTE = {
    "id": "11111111-1111-4111-8111-111111111111",
    "appointmentId": "22222222-2222-4222-8222-222222222222",
    "doctorId": "33333333-3333-4333-8333-333333333333",
    "content": "All good",
    "createdAt": "2026-01-01T00:00:00+00:00",
}

# A spec-valid VisitHistory with one nested appointment, to prove the client
# flattens nested models into the plain camelCase dicts context.py reads.
_VALID_VISIT_HISTORY = {
    "patientId": "44444444-4444-4444-8444-444444444444",
    "appointments": [
        {
            "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            "patientId": "44444444-4444-4444-8444-444444444444",
            "doctorId": "33333333-3333-4333-8333-333333333333",
            "dateTime": "2026-05-01T09:00:00+00:00",
            "status": "SCHEDULED",
            "duration": 20,
            "reason": "Check",
        }
    ],
}


def _run_with_status(status_code: int, json_body=_NO_JSON, call=None):
    """Drive a service_client call against a transport returning ``status_code``.

    ``call`` is a zero-arg factory returning the coroutine to run; it defaults to
    ``get_appointment_note`` so the status-handling tests stay terse.
    """

    def handler(_request: httpx.Request) -> httpx.Response:
        if json_body is _NO_JSON:
            return httpx.Response(status_code, content=b"")
        return httpx.Response(status_code, json=json_body)

    transport = httpx.MockTransport(handler)

    def client_factory(**kwargs):
        # Inject the mock transport so the generated client's request is served
        # locally without a real socket, while keeping the real base_url/headers.
        kwargs["httpx_args"] = {**kwargs.get("httpx_args", {}), "transport": transport}
        return Client(**kwargs)

    original = service_client.Client
    service_client.Client = client_factory
    try:
        coro = call() if call else service_client.get_appointment_note("a-1", headers={})
        return asyncio.run(coro)
    finally:
        service_client.Client = original


def test_returns_none_on_not_found():
    assert _run_with_status(httpx.codes.NOT_FOUND) is None


def test_returns_none_on_forbidden():
    # A note authored by a different doctor comes back 403; it must be skipped,
    # not raised, so a multi-doctor visit history doesn't fail the whole query.
    assert _run_with_status(httpx.codes.FORBIDDEN) is None


def test_returns_none_on_no_content():
    assert _run_with_status(httpx.codes.NO_CONTENT) is None


def test_returns_parsed_body_on_success():
    result = _run_with_status(httpx.codes.OK, _VALID_NOTE)
    assert result["content"] == "All good"
    # The parsed model round-trips back to the API's field names.
    assert result["appointmentId"] == _VALID_NOTE["appointmentId"]


def test_raises_on_server_error():
    # DownstreamError subclasses httpx.HTTPError, which the query route maps to 502.
    with pytest.raises(httpx.HTTPError):
        _run_with_status(httpx.codes.INTERNAL_SERVER_ERROR)


def test_visit_history_flattens_nested_appointments():
    # context.py consumes appointments as plain dicts (a.get("dateTime"), a["id"]);
    # verify the generated model's nested to_dict produces exactly that shape.
    result = _run_with_status(
        httpx.codes.OK,
        _VALID_VISIT_HISTORY,
        call=lambda: service_client.get_visit_history("p-1", headers={}),
    )
    assert isinstance(result["appointments"], list)
    appointment = result["appointments"][0]
    assert appointment["reason"] == "Check"
    assert appointment["status"] == "SCHEDULED"
    assert appointment["dateTime"].startswith("2026-05-01")
