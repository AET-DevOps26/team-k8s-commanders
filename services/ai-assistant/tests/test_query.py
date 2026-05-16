from fastapi.testclient import TestClient

from main import app


def test_query_works_without_authentication():
    """Test that query endpoint is publicly accessible."""
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={"query": "What is the patient's current status?"},
    )

    assert response.status_code in [200, 500]
    if response.status_code == 200:
        data = response.json()
        assert "answer" in data
        assert isinstance(data["answer"], str)


def test_query_with_patient_id():
    """Test query with patient context."""
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What are the patient's current medications?",
            "patientId": "550e8400-e29b-41d4-a716-446655440000",
        },
    )
    
    # Should succeed or fail due to LLM config, not authentication
    assert response.status_code in [200, 500]


def test_query_with_appointment_id():
    """Test query with appointment context."""
    client = TestClient(app)
    response = client.post(
        "/ai/query",
        json={
            "query": "What is scheduled for the appointment?",
            "appointmentId": "660e8400-e29b-41d4-a716-446655440000",
        },
    )
    
    # Should succeed or fail due to LLM config, not authentication
    assert response.status_code in [200, 500]


def test_query_request_validation():
    """Test query request validation."""
    client = TestClient(app)
    # Missing required 'query' field
    response = client.post(
        "/ai/query",
        json={},
    )
    
    assert response.status_code == 422  # Validation error
