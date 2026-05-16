import os
from dotenv import load_dotenv
import uvicorn
from fastapi import FastAPI

from routes import query, health

# Load environment variables from .env file
load_dotenv()

app = FastAPI(
    title="GenAI Service",
    docs_url=None,      # disable Swagger UI
    redoc_url=None,     # disable ReDoc
    openapi_url=None,   # disable OpenAPI schema endpoint
)

app.include_router(health.router, tags=["health"])
app.include_router(query.router, prefix="/ai", tags=["query"])

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,   # remove in production
    )