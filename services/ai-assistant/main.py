from contextlib import asynccontextmanager

from dotenv import load_dotenv
import uvicorn
from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from db.engine import init_models
from routes import sessions, health

load_dotenv()


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Create the conversation tables on startup (dev convenience; production
    # should manage schema with migrations).
    await init_models()
    yield


app = FastAPI(
    title="GenAI Service",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app, endpoint="/metrics")

app.include_router(health.router, prefix="/ai", tags=["health"])
app.include_router(sessions.router, prefix="/ai", tags=["sessions"])

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )
