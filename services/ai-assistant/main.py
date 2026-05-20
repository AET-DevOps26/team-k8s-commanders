from dotenv import load_dotenv
import uvicorn
from fastapi import FastAPI

from routes import query, health

load_dotenv()

app = FastAPI(
    title="GenAI Service",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)

app.include_router(health.router, prefix="/ai", tags=["health"])
app.include_router(query.router, prefix="/ai", tags=["query"])

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )
