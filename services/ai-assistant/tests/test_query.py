from unittest.mock import AsyncMock, patch

import httpx
from fastapi.testclient import TestClient
from langchain_core.documents import Document
from langchain_core.language_models.fake_chat_models import FakeListChatModel
from langchain_core.runnables import RunnableLambda

from main import app

# Role header injected by the API gateway after JWT validation.
DOCTOR_HEADERS = {"X-User-Role": "DOCTOR"}
ADMIN_HEADERS = {"X-User-Role": "ADMIN"}

# Grounding documents that build_context would produce from live service data.
PATIENT_DOC = Document(
    page_content="Patient: John Doe\nDate of birth: 1985-05-15",
    metadata={"source": "Patient record"},
)
NOTE_DOC = Document(
    page_content="Clinical note: Patient stable.\nDiagnosis: Hypertension (Code: I10)",
    metadata={"source": "Clinical note"},
)
APPOINTMENT_DOC = Document(
    page_content="Appointment (2026-01-10T09:00:00Z): Diabetes check-up\nStatus: SCHEDULED, Duration: 30 min",
    metadata={"source": "Appointment record"},
)


def _fake_llm(response: str) -> FakeListChatModel:
    return FakeListChatModel(responses=[response])


@patch("routes.query.get_llm")
@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_allowed_for_doctor(mock_build_context, mock_get_llm):
    """Test that DOCTOR role can query with mocked context + LLM."""
    mock_build_context.return_value = [PATIENT_DOC]
    mock_get_llm.return_value = _fake_llm(
        "The patient appears to be in stable condition."
    )

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={
            "query": "What is the patient's current status?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["answer"] == "The patient appears to be in stable condition."
    assert "sources" in data
    assert isinstance(data["sources"], list)
    assert "confidence" in data


@patch("routes.query.get_llm")
@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_with_patient_id(mock_build_context, mock_get_llm):
    """Test query with patient context."""
    mock_build_context.return_value = [PATIENT_DOC, NOTE_DOC]
    mock_get_llm.return_value = _fake_llm(
        "Current medications: Lisinopril 10mg daily, Metformin 500mg twice daily."
    )

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={
            "query": "What are the patient's current medications?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert "Current medications" in data["answer"]


@patch("routes.query.get_llm")
@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_with_appointment_id(mock_build_context, mock_get_llm):
    """Test query with appointment context."""
    mock_build_context.return_value = [APPOINTMENT_DOC]
    mock_get_llm.return_value = _fake_llm(
        "The appointment is scheduled for a diabetes check-up."
    )

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={
            "query": "What is scheduled for the appointment?",
            "appointmentId": "660e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert "appointment" in data["answer"].lower()


@patch("routes.query.get_llm")
@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_with_both_ids(mock_build_context, mock_get_llm):
    """Test query with both patient and appointment context."""
    mock_build_context.return_value = [PATIENT_DOC, NOTE_DOC, APPOINTMENT_DOC]
    expected = "Based on the patient history and appointment details, the recommended action is..."
    mock_get_llm.return_value = _fake_llm(expected)

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=ADMIN_HEADERS,
        json={
            "query": "What should be the focus of this appointment?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
            "appointmentId": "660e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["answer"] == expected
    assert set(data["sources"]) == {
        "Patient record",
        "Clinical note",
        "Appointment record",
    }


def test_query_request_validation():
    """Test query request validation."""
    client = TestClient(app)
    response = client.post("/ai/query", headers=DOCTOR_HEADERS, json={})
    assert response.status_code == 422


@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_missing_context_raises_404(mock_build_context):
    """Test that unknown patient/appointment IDs (no data) return 404."""
    mock_build_context.return_value = []

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={
            "query": "What is the status?",
            "patientId": "00000000-0000-0000-0000-000000000000",
            "appointmentId": "00000000-0000-0000-0000-000000000001",
        },
    )

    assert response.status_code == 404
    assert "detail" in response.json()


@patch("routes.query.get_llm")
@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_without_ids_answers_generally(mock_build_context, mock_get_llm):
    """No patient/appointment id: answer as a general medical assistant, not 404."""
    mock_build_context.return_value = []
    mock_get_llm.return_value = _fake_llm(
        "Metformin is a first-line oral medication for type 2 diabetes."
    )

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={"query": "What is the first-line treatment for type 2 diabetes?"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["answer"] == "Metformin is a first-line oral medication for type 2 diabetes."
    # No grounding documents, so no sources are cited.
    assert data["sources"] == []


@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_upstream_failure_raises_502(mock_build_context):
    """An upstream service error while fetching context surfaces as 502."""
    mock_build_context.side_effect = httpx.ConnectError("connection refused")

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={
            "query": "What is the status?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 502


@patch("routes.query.get_llm")
@patch("routes.query.build_context", new_callable=AsyncMock)
def test_query_llm_error_handling(mock_build_context, mock_get_llm):
    """Test that LLM generation errors are handled gracefully."""
    mock_build_context.return_value = [PATIENT_DOC]

    async def _raise(_input):
        raise Exception("LLM service temporarily unavailable")

    mock_get_llm.return_value = RunnableLambda(_raise)

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers=DOCTOR_HEADERS,
        json={
            "query": "What is the status?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 500
    assert "error processing query" in response.json()["detail"].lower()


_AUTH_PAYLOAD = {
    "query": "What is the patient's current status?",
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
}


def test_query_forbidden_for_patient():
    """PATIENT role is authenticated but not allowed to query the assistant."""
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers={"X-User-Role": "PATIENT"},
        json=_AUTH_PAYLOAD,
    )

    assert response.status_code == 403


def test_query_unauthorized_without_role_header():
    """A missing role header (request not forwarded by the gateway) is rejected."""
    client = TestClient(app)
    response = client.post("/ai/query", json=_AUTH_PAYLOAD)

    assert response.status_code == 401


def test_query_forbidden_for_unknown_role():
    """An unrecognized role value is rejected."""
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        headers={"X-User-Role": "SUPERHERO"},
        json=_AUTH_PAYLOAD,
    )

    assert response.status_code == 403
