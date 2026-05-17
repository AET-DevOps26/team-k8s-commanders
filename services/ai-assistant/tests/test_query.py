from fastapi.testclient import TestClient

from main import app


def test_query_route_returns_placeholder_message():
    client = TestClient(app)
    response = client.post("/ai/query")

    assert response.status_code == 200
    assert response.json() == {
        "message": "This is a placeholder for the query endpoint."
    }
