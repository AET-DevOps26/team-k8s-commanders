from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient
from pydantic import BaseModel

from errors import install_exception_handlers


def _app() -> FastAPI:
    app = FastAPI()
    install_exception_handlers(app)

    class Payload(BaseModel):
        value: int

    @app.get("/missing")
    async def missing():
        raise HTTPException(
            status_code=404,
            detail="Session not found",
            headers={"X-Error-Code": "SESSION_NOT_FOUND"},
        )

    @app.post("/validated")
    async def validated(payload: Payload):
        return payload

    @app.get("/unexpected")
    async def unexpected():
        raise RuntimeError("database password leaked")

    return app


def test_http_exception_uses_problem_details():
    with TestClient(_app()) as client:
        response = client.get("/missing")

    assert response.status_code == 404
    assert response.headers["content-type"].startswith("application/problem+json")
    assert response.headers["x-error-code"] == "SESSION_NOT_FOUND"
    assert response.json() == {
        "type": "about:blank",
        "title": "Not Found",
        "status": 404,
        "detail": "Session not found",
        "instance": "/missing",
    }


def test_validation_exception_contains_field_errors():
    with TestClient(_app()) as client:
        response = client.post("/validated", json={"value": "invalid"})

    assert response.status_code == 400
    assert response.headers["content-type"].startswith("application/problem+json")
    assert response.json()["title"] == "Bad Request"
    assert response.json()["status"] == 400
    assert response.json()["detail"] == "Request validation failed"
    assert response.json()["errors"] == [
        {
            "field": "body.value",
            "message": "Input should be a valid integer, unable to parse string as an integer",
        }
    ]


def test_unexpected_exception_hides_internal_detail():
    with TestClient(_app(), raise_server_exceptions=False) as client:
        response = client.get("/unexpected")

    assert response.status_code == 500
    assert response.headers["content-type"].startswith("application/problem+json")
    assert response.json()["detail"] == "An unexpected error occurred"
    assert "database password leaked" not in response.text
