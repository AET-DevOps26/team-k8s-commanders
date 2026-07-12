"""Typed clients for the source-of-truth backend services.

The assistant grounds its answers in live patient data rather than a vector
store, so it fetches that data straight from the services that own it:
patient-service (profiles, appointments, visit history) and notes-service
(clinical notes).

Every call goes through the generated ``caredesk_client`` — an httpx client
derived from ``api/openapi.yaml`` — so request paths and response shapes stay
in sync with the contract and there are no hand-written HTTP calls. The parsed
models are flattened back to plain dicts here so the context assembly stays
dict-based.

The gateway-issued identity headers are forwarded on each call so those
services authenticate the assistant exactly as they would a direct client
request — see their ``*HeaderAuthFilter``; those headers are only ever set by
the gateway at the edge.
"""

import os

import httpx

from caredesk_client import Client
from caredesk_client.api.appointments import get_appointment_by_id as _ep_appointment
from caredesk_client.api.appointments import get_appointment_note as _ep_note
from caredesk_client.api.patients import get_patient_by_id as _ep_profile
from caredesk_client.api.patients import get_patient_visit_history as _ep_history
from caredesk_client.models.appointment import Appointment
from caredesk_client.models.clinical_note import ClinicalNote
from caredesk_client.models.user_profile import UserProfile
from caredesk_client.models.visit_history import VisitHistory

# Identity headers set by the gateway after it validates the JWT. We forward
# whichever of these are present on the incoming request so the downstream
# service sees the same caller.
_FORWARDED_HEADERS = ("x-user-email", "x-user-role", "x-user-id")

_TIMEOUT = 10.0

# Statuses that mean "no data this caller can use" rather than an error: 404
# (missing), 403 (exists but not visible to this caller), 204 (known resource
# with no body). A 403 matters when a patient's visit history spans multiple
# doctors: notes authored by a *different* doctor come back 403 from
# notes-service, and skipping them here keeps one inaccessible note from
# failing the whole query while leaking nothing the caller couldn't already
# read.
_SKIP_STATUSES = frozenset(
    {httpx.codes.NOT_FOUND, httpx.codes.FORBIDDEN, httpx.codes.NO_CONTENT}
)


class DownstreamError(httpx.HTTPError):
    """A backend service returned an unexpected error status.

    Subclasses ``httpx.HTTPError`` so the query route maps it to a 502 exactly
    like a connection failure does.
    """


def _patient_base() -> str:
    return os.getenv("PATIENT_SERVICE_URL", "http://localhost:8082")


def _notes_base() -> str:
    return os.getenv("NOTES_SERVICE_URL", "http://localhost:8083")


def forwarded_headers(incoming) -> dict:
    """Select the identity headers to forward downstream.

    Accepts any case-insensitive header mapping (e.g. Starlette's ``Headers``)
    and returns a plain dict containing only the trusted identity headers that
    were present.
    """
    lowered = {k.lower(): v for k, v in incoming.items()}
    return {key: lowered[key] for key in _FORWARDED_HEADERS if key in lowered}


def _client(base_url: str, headers: dict) -> Client:
    """Build a generated client for one service, forwarding identity headers."""
    return Client(
        base_url=base_url,
        headers=headers,
        raise_on_unexpected_status=False,
        timeout=httpx.Timeout(_TIMEOUT),
    )


async def _fetch(base_url: str, headers: dict, endpoint, model, resource_id: str):
    """Fetch one resource through the generated client and return it as a dict.

    We dispatch on the status *before* parsing rather than calling the
    endpoint's ``asyncio_detailed`` helper: a 404/403/204 must be skipped
    (``None``) regardless of the response body — notably a patient's AI query
    triggers a 403 from notes-service for every clinical note, and that 403 has
    no ``problem+json`` body to parse. The endpoint's own path builder keeps the
    URL generated from the spec, and only successful bodies are parsed (through
    the generated model), so a missing/empty body never fails the whole query.
    Any other non-2xx raises :class:`DownstreamError`, which the route maps to
    a 502.
    """
    async with _client(base_url, headers) as client:
        response = await client.get_async_httpx_client().request(
            **endpoint._get_kwargs(resource_id)
        )
        status = response.status_code
        if status in _SKIP_STATUSES:
            return None
        if not 200 <= status < 300:
            raise DownstreamError(f"backend service returned HTTP {status}")
        if not response.content:
            return None
        try:
            result = model.from_dict(response.json()).to_dict()
        except (ValueError, KeyError, TypeError) as e:
            # Malformed JSON (ValueError) or a body that doesn't match the
            # generated model's schema (missing keys / bad UUIDs / wrong types)
            # is an upstream fault — surface it as a DownstreamError so the route
            # keeps its documented 502 mapping instead of leaking an unhandled 500.
            raise DownstreamError(
                f"backend service returned an unparseable or invalid body: {e}"
            ) from e
        # Defense-in-depth: the shared UserProfile schema declares a writeOnly
        # `password` field (it doubles as the update-user request body). The
        # generated client ignores writeOnly and would round-trip that value
        # straight into the LLM context if a backend response ever carried it —
        # so never propagate it, regardless of the model being fetched.
        result.pop("password", None)
        return result


async def get_patient_profile(patient_id: str, headers: dict):
    """Fetch a patient's profile (demographics) from patient-service."""
    return await _fetch(_patient_base(), headers, _ep_profile, UserProfile, patient_id)


async def get_visit_history(patient_id: str, headers: dict):
    """Fetch a patient's visit history (appointments + notes) from patient-service."""
    return await _fetch(_patient_base(), headers, _ep_history, VisitHistory, patient_id)


async def get_appointment(appointment_id: str, headers: dict):
    """Fetch a single appointment from patient-service."""
    return await _fetch(_patient_base(), headers, _ep_appointment, Appointment, appointment_id)


async def get_appointment_note(appointment_id: str, headers: dict):
    """Fetch the clinical note for an appointment from notes-service."""
    return await _fetch(_notes_base(), headers, _ep_note, ClinicalNote, appointment_id)
