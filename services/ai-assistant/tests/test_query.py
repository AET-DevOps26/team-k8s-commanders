from unittest.mock import AsyncMock, patch
from fastapi.testclient import TestClient

from main import app


@patch("routes.query.get_llm_provider")
def test_query_works_without_authentication(mock_get_provider):
    """Test that query endpoint is publicly accessible with mocked LLM."""
    mock_provider = AsyncMock()
    mock_provider.generate.return_value = "The patient appears to be in stable condition."
    mock_get_provider.return_value = mock_provider

    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What is the patient's current status?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
    assert isinstance(data["answer"], str)
    assert data["answer"] == "The patient appears to be in stable condition."
    assert "sources" in data
    assert isinstance(data["sources"], list)
    assert "confidence" in data


@patch("routes.query.get_llm_provider")
def test_query_with_patient_id(mock_get_provider):
    """Test query with patient context."""
    mock_provider = AsyncMock()
    mock_provider.generate.return_value = "Current medications: Lisinopril 10mg daily, Metformin 500mg twice daily."
    mock_get_provider.return_value = mock_provider
    
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What are the patient's current medications?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )
    
    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
    assert "Current medications" in data["answer"]
    # Verify that the LLM provider was called (indicating RAG context was built)
    assert mock_provider.generate.called
    call_args = mock_provider.generate.call_args[0][0]  # Get the prompt argument
    assert "550e8400-e29b-41d4-a716-446655440000" in call_args or "Patient" in call_args


@patch("routes.query.get_llm_provider")
def test_query_with_appointment_id(mock_get_provider):
    """Test query with appointment context."""
    mock_provider = AsyncMock()
    mock_provider.generate.return_value = "The appointment is scheduled for preventive care and routine checkup."
    mock_get_provider.return_value = mock_provider
    
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What is scheduled for the appointment?",
            "appointmentId": "660e8400-e29b-41d4-a716-446655440000",
        },
    )
    
    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
    # Verify that the LLM provider was called with context
    assert mock_provider.generate.called
    call_args = mock_provider.generate.call_args[0][0]
    assert "Appointment" in call_args


@patch("routes.query.get_llm_provider")
def test_query_with_both_ids(mock_get_provider):
    """Test query with both patient and appointment context."""
    mock_provider = AsyncMock()
    expected_answer = "Based on the patient history and appointment details, the recommended action is..."
    mock_provider.generate.return_value = expected_answer
    mock_get_provider.return_value = mock_provider
    
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What should be the focus of this appointment?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
            "appointmentId": "660e8400-e29b-41d4-a716-446655440000",
        },
    )
    
    assert response.status_code == 200
    data = response.json()
    assert data["answer"] == expected_answer
    assert data["sources"] == ["Patient history", "Clinical notes", "Appointment records"]


def test_query_request_validation():
    """Test query request validation."""
    client = TestClient(app)
    # Missing required 'query' field
    response = client.post(
        "/ai/query",
        json={},
    )
    
    assert response.status_code == 422  # Validation error


@patch("routes.query.get_llm_provider")
def test_query_missing_context_raises_404(mock_get_provider):
    """Test that unknown patient/appointment IDs return 404."""
    mock_provider = AsyncMock()
    mock_get_provider.return_value = mock_provider

    client = TestClient(app)
    # Valid UUID format, but not present in mock data
    response = client.post(
        "/ai/query",
        json={
            "query": "What is the status?",
            "patientId": "00000000-0000-0000-0000-000000000000",
            "appointmentId": "00000000-0000-0000-0000-000000000001",
        },
    )

    assert response.status_code == 404
    data = response.json()
    assert "detail" in data


@patch("routes.query.get_llm_provider")
def test_query_llm_error_handling(mock_get_provider):
    """Test that LLM generation errors are handled gracefully."""
    mock_provider = AsyncMock()
    mock_provider.generate.side_effect = Exception("LLM service temporarily unavailable")
    mock_get_provider.return_value = mock_provider
    
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What is the status?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )
    
    assert response.status_code == 500
    data = response.json()
    assert "detail" in data
    assert "error processing query" in data["detail"].lower()
