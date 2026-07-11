"""Tests for persistent AI conversation sessions.

The app is driven with ``with TestClient(app) as client`` so the FastAPI
lifespan runs and creates the tables in the per-test SQLite DB (see conftest).
``get_llm`` and ``build_context`` are mocked exactly as the old query tests did.
"""

import json
from unittest.mock import AsyncMock, patch

import httpx
from fastapi.testclient import TestClient
from langchain_core.documents import Document
from langchain_core.language_models.fake_chat_models import FakeListChatModel
from langchain_core.runnables import RunnableLambda

from main import app

# Identity headers injected by the API gateway after JWT validation.
USER_A = "11111111-1111-1111-1111-111111111111"
USER_B = "22222222-2222-2222-2222-222222222222"
DOCTOR_HEADERS = {"X-User-Role": "DOCTOR", "X-User-Id": USER_A}
ADMIN_HEADERS = {"X-User-Role": "ADMIN", "X-User-Id": USER_A}
USER_B_HEADERS = {"X-User-Role": "DOCTOR", "X-User-Id": USER_B}

PATIENT_ID = "550e8400-e29b-41d4-a716-446655440000"
APPOINTMENT_ID = "660e8400-e29b-41d4-a716-446655440000"

PATIENT_DOC = Document(
    page_content="Patient: John Doe\nDate of birth: 1985-05-15",
    metadata={"source": "Patient record"},
)
NOTE_DOC = Document(
    page_content="Clinical note: Patient stable.\nDiagnosis: Hypertension (Code: I10)",
    metadata={"source": "Clinical note"},
)
GUIDELINE_DOC = Document(
    page_content="Step 1 antihypertensive: ACE inhibitor or ARB.",
    metadata={"source": "Clinical guideline: Hypertension"},
)


def _fake_llm(response: str) -> FakeListChatModel:
    return FakeListChatModel(responses=[response])


def _client() -> TestClient:
    # Context-manager form runs the lifespan, which creates the tables.
    return TestClient(app)


# ── Session CRUD ─────────────────────────────────────────────────────────────


def test_create_session_returns_201_with_empty_messages():
    with _client() as client:
        response = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        )

    assert response.status_code == 201
    data = response.json()
    assert data["userId"] == USER_A
    assert data["patientId"] == PATIENT_ID
    assert data["messages"] == []
    assert "id" in data


def test_create_session_without_binding():
    with _client() as client:
        response = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"title": "General questions"}
        )

    assert response.status_code == 201
    data = response.json()
    assert data["title"] == "General questions"
    assert data.get("patientId") is None


def test_get_session_returns_messages():
    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={}
        ).json()["id"]

        response = client.get(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS)

    assert response.status_code == 200
    assert response.json()["id"] == sid


def test_list_sessions_only_returns_callers_own():
    with _client() as client:
        client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={})
        client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={})
        client.post("/ai/sessions", headers=USER_B_HEADERS, json={})

        response = client.get("/ai/sessions", headers=DOCTOR_HEADERS)

    assert response.status_code == 200
    body = response.json()
    assert body["page"]["totalElements"] == 2
    assert len(body["content"]) == 2
    assert all(s["userId"] == USER_A for s in body["content"])


def test_delete_session():
    with _client() as client:
        sid = client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={}).json()["id"]

        deleted = client.delete(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS)
        missing = client.get(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS)

    assert deleted.status_code == 204
    assert missing.status_code == 404


# ── Ownership isolation ──────────────────────────────────────────────────────


def test_other_user_cannot_read_session():
    with _client() as client:
        sid = client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={}).json()["id"]

        response = client.get(f"/ai/sessions/{sid}", headers=USER_B_HEADERS)

    assert response.status_code == 404


def test_other_user_cannot_delete_session():
    with _client() as client:
        sid = client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={}).json()["id"]

        response = client.delete(f"/ai/sessions/{sid}", headers=USER_B_HEADERS)

    assert response.status_code == 404


# ── Message turns ────────────────────────────────────────────────────────────


@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_message_persists_user_and_assistant(mock_build_context, mock_get_llm):
    mock_build_context.return_value = [PATIENT_DOC, NOTE_DOC]
    mock_get_llm.return_value = _fake_llm("The patient is stable.")

    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]

        reply = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "How is the patient?"},
        )
        session = client.get(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS).json()

    assert reply.status_code == 200
    assert reply.json()["answer"] == "The patient is stable."
    assert set(reply.json()["sources"]) == {"Patient record", "Clinical note"}

    messages = session["messages"]
    assert len(messages) == 2
    assert messages[0]["role"] == "user"
    assert messages[0]["content"] == "How is the patient?"
    assert messages[1]["role"] == "assistant"
    assert messages[1]["content"] == "The patient is stable."
    assert messages[1]["sources"] == ["Patient record", "Clinical note"]


@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_prior_conversation_is_threaded_into_prompt(mock_build_context, mock_get_llm):
    """The second turn must replay the first turn's messages to the model."""
    mock_build_context.return_value = []

    captured = {}

    def _capture(messages):
        # ``messages`` is the rendered prompt (a list of chat messages).
        captured["contents"] = [m.content for m in messages.to_messages()]
        return "ok"

    mock_get_llm.return_value = RunnableLambda(_capture)

    with _client() as client:
        sid = client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={}).json()["id"]
        client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "first question"},
        )
        client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "second question"},
        )

    contents = captured["contents"]
    assert "first question" in contents
    assert "ok" in contents  # the assistant's first reply was replayed
    assert contents[-1] == "second question"


@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_message_on_missing_session_404(mock_build_context):
    mock_build_context.return_value = []
    with _client() as client:
        response = client.post(
            "/ai/sessions/00000000-0000-0000-0000-000000000000/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "hi"},
        )
    assert response.status_code == 404


@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_bound_ids_resolving_to_nothing_404(mock_build_context):
    mock_build_context.return_value = []
    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]

        response = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "hi"},
        )
    assert response.status_code == 404


@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_upstream_failure_502(mock_build_context):
    mock_build_context.side_effect = httpx.ConnectError("connection refused")
    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]

        response = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "hi"},
        )
    assert response.status_code == 502


@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_unbound_session_answers_generally(mock_build_context, mock_get_llm):
    mock_build_context.return_value = []
    mock_get_llm.return_value = _fake_llm(
        "Metformin is a first-line oral medication for type 2 diabetes."
    )
    with _client() as client:
        sid = client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={}).json()["id"]
        response = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "First-line treatment for type 2 diabetes?"},
        )

    assert response.status_code == 200
    assert response.json()["sources"] == []


@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_message_llm_error_500_and_nothing_persisted(mock_build_context, mock_get_llm):
    mock_build_context.return_value = [PATIENT_DOC]

    async def _raise(_input):
        raise Exception("LLM service temporarily unavailable")

    mock_get_llm.return_value = RunnableLambda(_raise)

    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]
        response = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "status?"},
        )
        session = client.get(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS).json()

    assert response.status_code == 500
    # A failed non-streaming turn persists neither message.
    assert session["messages"] == []


# ── Auth ─────────────────────────────────────────────────────────────────────


def test_missing_role_header_401():
    with _client() as client:
        response = client.post(
            "/ai/sessions", headers={"X-User-Id": USER_A}, json={}
        )
    assert response.status_code == 401


def test_missing_user_id_header_401():
    with _client() as client:
        response = client.post(
            "/ai/sessions", headers={"X-User-Role": "DOCTOR"}, json={}
        )
    assert response.status_code == 401


def test_patient_role_forbidden():
    with _client() as client:
        response = client.post(
            "/ai/sessions",
            headers={"X-User-Role": "PATIENT", "X-User-Id": USER_A},
            json={},
        )
    assert response.status_code == 403


def test_admin_allowed():
    with _client() as client:
        response = client.post("/ai/sessions", headers=ADMIN_HEADERS, json={})
    assert response.status_code == 201


# ── Clinical-guidelines RAG ──────────────────────────────────────────────────


@patch("routes.sessions.retrieve_guidelines", new_callable=AsyncMock)
@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_guideline_sources_merged_and_text_reaches_prompt(
    mock_build_context, mock_get_llm, mock_retrieve
):
    """Retrieved guidelines surface in sources and their text is in the prompt."""
    mock_build_context.return_value = [PATIENT_DOC]
    mock_retrieve.return_value = [GUIDELINE_DOC]

    captured = {}

    def _capture(messages):
        captured["contents"] = [m.content for m in messages.to_messages()]
        return "Per guideline, start an ACE inhibitor."

    mock_get_llm.return_value = RunnableLambda(_capture)

    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]
        reply = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "How should I treat the hypertension?"},
        )

    assert reply.status_code == 200
    # Guideline label is merged in alongside the patient source.
    assert reply.json()["sources"] == ["Patient record", "Clinical guideline: Hypertension"]
    # The guideline excerpt was injected into the system prompt.
    system_message = captured["contents"][0]
    assert "ACE inhibitor or ARB" in system_message


@patch("routes.sessions.retrieve_guidelines", new_callable=AsyncMock)
@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_guidelines_enrich_unbound_session(
    mock_build_context, mock_get_llm, mock_retrieve
):
    """A session with no patient binding still gets guideline grounding."""
    mock_build_context.return_value = []
    mock_retrieve.return_value = [GUIDELINE_DOC]
    mock_get_llm.return_value = _fake_llm("General hypertension guidance.")

    with _client() as client:
        sid = client.post("/ai/sessions", headers=DOCTOR_HEADERS, json={}).json()["id"]
        reply = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=DOCTOR_HEADERS,
            json={"query": "First-line antihypertensive?"},
        )

    assert reply.status_code == 200
    assert reply.json()["sources"] == ["Clinical guideline: Hypertension"]


# ── Streaming (Server-Sent Events) ───────────────────────────────────────────
SSE_HEADERS = {**DOCTOR_HEADERS, "Accept": "text/event-stream"}


def _parse_sse(text: str):
    events = []
    for block in text.strip().split("\n\n"):
        if not block.strip():
            continue
        event, data = None, None
        for line in block.splitlines():
            if line.startswith("event:"):
                event = line[len("event:"):].strip()
            elif line.startswith("data:"):
                data = json.loads(line[len("data:"):].strip())
        events.append((event, data))
    return events


@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_message_streams_and_persists_answer(mock_build_context, mock_get_llm):
    mock_build_context.return_value = [PATIENT_DOC, NOTE_DOC]
    mock_get_llm.return_value = _fake_llm("The patient is stable.")

    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]

        response = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=SSE_HEADERS,
            json={"query": "Status?"},
        )

        assert response.status_code == 200
        assert response.headers["content-type"].startswith("text/event-stream")
        events = _parse_sse(response.text)
        assert events[0][0] == "sources"
        assert events[0][1] == ["Patient record", "Clinical note"]
        assert events[-1] == ("done", {})
        answer = "".join(data for event, data in events if event == "token")
        assert answer == "The patient is stable."

        session = client.get(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS).json()

    messages = session["messages"]
    assert [m["role"] for m in messages] == ["user", "assistant"]
    assert messages[1]["content"] == "The patient is stable."


@patch("routes.sessions.get_llm")
@patch("routes.sessions.build_context", new_callable=AsyncMock)
def test_stream_llm_error_reported_as_event(mock_build_context, mock_get_llm):
    mock_build_context.return_value = [PATIENT_DOC]

    def _raise(_input):
        raise RuntimeError("boom")

    mock_get_llm.return_value = RunnableLambda(_raise)

    with _client() as client:
        sid = client.post(
            "/ai/sessions", headers=DOCTOR_HEADERS, json={"patientId": PATIENT_ID}
        ).json()["id"]
        response = client.post(
            f"/ai/sessions/{sid}/messages",
            headers=SSE_HEADERS,
            json={"query": "Status?"},
        )
        session = client.get(f"/ai/sessions/{sid}", headers=DOCTOR_HEADERS).json()

    assert response.status_code == 200
    events = _parse_sse(response.text)
    assert events[0][0] == "sources"
    assert events[-1][0] == "error"
    # A failed streaming turn persists neither message, so retrying does not
    # duplicate the user's question in the session history.
    assert session["messages"] == []
