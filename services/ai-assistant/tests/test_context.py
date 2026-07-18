"""Unit tests for live-context assembly and header forwarding."""

import asyncio
from unittest.mock import AsyncMock, patch

from utils import context
from utils.service_client import forwarded_headers


def test_forwarded_headers_selects_only_trusted_context_headers():
    incoming = {
        "X-User-Email": "doc@clinic.com",
        "X-User-Role": "DOCTOR",
        "X-User-Id": "abc-123",
        "X-Correlation-ID": "request-123",
        "Authorization": "Bearer secret",
        "Content-Type": "application/json",
    }
    result = forwarded_headers(incoming)
    assert result == {
        "x-user-email": "doc@clinic.com",
        "x-user-role": "DOCTOR",
        "x-user-id": "abc-123",
        "x-correlation-id": "request-123",
    }


@patch("utils.context.service_client.get_visit_history", new_callable=AsyncMock)
@patch("utils.context.service_client.get_patient_profile", new_callable=AsyncMock)
def test_build_context_patient_combines_profile_and_history(mock_profile, mock_history):
    mock_profile.return_value = {"name": "John Doe", "dateOfBirth": "1985-05-15"}
    mock_history.return_value = {
        "appointments": [
            {"dateTime": "2026-01-01T09:00:00Z", "reason": "Old", "status": "COMPLETED", "duration": 30},
            {"dateTime": "2026-05-01T09:00:00Z", "reason": "Recent", "status": "SCHEDULED", "duration": 20},
        ],
        "notes": [
            {"content": "Stable", "diagnosis": {"description": "Hypertension", "code": "I10"}},
        ],
    }

    docs = asyncio.run(context.build_context("p-1", None, headers={}))

    sources = [d.metadata["source"] for d in docs]
    assert sources == [
        "Patient record",
        "Appointment record",  # most recent first
        "Appointment record",
        "Clinical note",
    ]
    # Appointments sorted most-recent-first.
    assert "Recent" in docs[1].page_content
    assert "Old" in docs[2].page_content
    # Diagnosis is rendered into the note text.
    assert "Hypertension (Code: I10)" in docs[3].page_content


@patch("utils.context.service_client.get_appointment_note", new_callable=AsyncMock)
@patch("utils.context.service_client.get_visit_history", new_callable=AsyncMock)
@patch("utils.context.service_client.get_patient_profile", new_callable=AsyncMock)
def test_build_context_patient_fetches_note_per_appointment(mock_profile, mock_history, mock_note):
    # patient-service returns appointments (with ids) but no inline notes; each
    # appointment's clinical note is fetched from notes-service by id.
    mock_profile.return_value = {"name": "Jane Roe"}
    mock_history.return_value = {
        "appointments": [
            {"id": "a-1", "dateTime": "2026-01-01T09:00:00Z", "reason": "Old", "status": "COMPLETED", "duration": 30},
            {"id": "a-2", "dateTime": "2026-05-01T09:00:00Z", "reason": "Recent", "status": "SCHEDULED", "duration": 20},
        ],
        "notes": [],
    }
    mock_note.side_effect = lambda appointment_id, headers: {
        "a-1": {"content": "Old visit note", "diagnosis": None},
        "a-2": {"content": "Recent visit note", "diagnosis": None},
    }[appointment_id]

    docs = asyncio.run(context.build_context("p-1", None, headers={}))

    assert [d.metadata["source"] for d in docs] == [
        "Patient record",
        "Appointment record",  # a-2, most recent first
        "Appointment record",  # a-1
        "Clinical note",
        "Clinical note",
    ]
    # A note is fetched for every appointment, in the (recent-first) appointment order.
    assert mock_note.await_count == 2
    assert "Recent visit note" in docs[3].page_content
    assert "Old visit note" in docs[4].page_content


@patch("utils.context.service_client.get_appointment_note", new_callable=AsyncMock)
@patch("utils.context.service_client.get_visit_history", new_callable=AsyncMock)
@patch("utils.context.service_client.get_patient_profile", new_callable=AsyncMock)
def test_build_context_patient_skips_appointments_without_a_note(mock_profile, mock_history, mock_note):
    mock_profile.return_value = {"name": "Jane Roe"}
    mock_history.return_value = {
        "appointments": [{"id": "a-1", "dateTime": "2026-05-01T09:00:00Z", "reason": "Check", "status": "SCHEDULED", "duration": 20}],
        "notes": [],
    }
    mock_note.return_value = None  # no note recorded for this appointment yet

    docs = asyncio.run(context.build_context("p-1", None, headers={}))

    assert [d.metadata["source"] for d in docs] == ["Patient record", "Appointment record"]


@patch("utils.context.service_client.get_appointment_note", new_callable=AsyncMock)
@patch("utils.context.service_client.get_appointment", new_callable=AsyncMock)
def test_build_context_appointment_fetches_appointment_and_note(mock_appointment, mock_note):
    mock_appointment.return_value = {"dateTime": "2026-05-01T09:00:00Z", "reason": "Check-up", "status": "SCHEDULED", "duration": 30}
    mock_note.return_value = {"content": "All good", "diagnosis": None}

    docs = asyncio.run(context.build_context(None, "a-1", headers={}))

    assert [d.metadata["source"] for d in docs] == ["Appointment record", "Clinical note"]
    mock_appointment.assert_awaited_once_with("a-1", {})
    mock_note.assert_awaited_once_with("a-1", {})


@patch("utils.context.service_client.get_visit_history", new_callable=AsyncMock)
@patch("utils.context.service_client.get_patient_profile", new_callable=AsyncMock)
def test_build_context_returns_empty_when_nothing_found(mock_profile, mock_history):
    mock_profile.return_value = None
    mock_history.return_value = None

    assert asyncio.run(context.build_context("p-unknown", None, headers={})) == []
