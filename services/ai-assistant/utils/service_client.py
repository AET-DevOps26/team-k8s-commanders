"""HTTP clients for the source-of-truth backend services.

The assistant grounds its answers in live patient data rather than a vector
store, so it fetches that data straight from the services that own it:
patient-service (profiles, appointments, visit history) and notes-service
(clinical notes). The gateway-issued identity headers are forwarded on each
call so those services authenticate it exactly as they would a direct client
request — see their ``*HeaderAuthFilter``; those headers are only ever set by
the gateway at the edge.
"""

import os

import httpx

# Identity headers set by the gateway after it validates the JWT. We forward
# whichever of these are present on the incoming request so the downstream
# service sees the same caller.
_FORWARDED_HEADERS = ("x-user-email", "x-user-role", "x-user-id")

_TIMEOUT = httpx.Timeout(10.0)


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


async def _get_json(base_url: str, path: str, headers: dict):
    """GET ``base_url + path`` and return parsed JSON, or ``None`` if the caller
    can't see the resource.

    A 404 (missing) and a 403 (exists but not visible to this caller) are both
    treated as "no data the caller can see" and return ``None`` so the resource
    is simply skipped from grounding. This matters when a patient's visit
    history spans multiple doctors: notes authored by a *different* doctor come
    back 403 from notes-service, and swallowing that here keeps one inaccessible
    note from failing the whole query while leaking nothing the caller couldn't
    already read.

    Raises ``httpx.HTTPError`` for connection failures and other error
    statuses, which the caller maps to a 502.
    """
    async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
        response = await client.get(f"{base_url}{path}", headers=headers)
        if response.status_code in (httpx.codes.NOT_FOUND, httpx.codes.FORBIDDEN):
            return None
        response.raise_for_status()
        return response.json()


async def get_patient_profile(patient_id: str, headers: dict):
    """Fetch a patient's profile (demographics) from patient-service."""
    return await _get_json(_patient_base(), f"/patients/{patient_id}", headers)


async def get_visit_history(patient_id: str, headers: dict):
    """Fetch a patient's visit history (appointments + notes) from patient-service."""
    return await _get_json(_patient_base(), f"/patients/{patient_id}/visit-history", headers)


async def get_appointment(appointment_id: str, headers: dict):
    """Fetch a single appointment from patient-service."""
    return await _get_json(_patient_base(), f"/appointments/{appointment_id}", headers)


async def get_appointment_note(appointment_id: str, headers: dict):
    """Fetch the clinical note for an appointment from notes-service."""
    return await _get_json(_notes_base(), f"/appointments/{appointment_id}/note", headers)
