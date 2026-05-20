from fastapi.testclient import TestClient

from main import app


def test_health_returns_200():
    client = TestClient(app)
    response = client.get("/ai/health")
    assert response.status_code == 200


def test_health_returns_expected_body():
    client = TestClient(app)
    response = client.get("/ai/health")
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "GenAI Service"
