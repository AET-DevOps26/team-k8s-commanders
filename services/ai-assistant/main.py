from contextlib import asynccontextmanager
from dotenv import load_dotenv
import uvicorn
from fastapi import FastAPI

from routes import query, health
from utils.llm import initialize_llm_provider

load_dotenv()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage application lifespan: startup and shutdown events."""
    initialize_llm_provider()
    yield


app = FastAPI(
    title="GenAI Service",
    docs_url=None,      # disable Swagger UI
    redoc_url=None,     # disable ReDoc
    openapi_url=None,   # disable OpenAPI schema endpoint
    lifespan=lifespan,
)

app.include_router(health.router, tags=["health"])
app.include_router(query.router, prefix="/ai", tags=["query"])

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )