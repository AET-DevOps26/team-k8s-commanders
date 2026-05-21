"""Health check endpoint."""

from fastapi import APIRouter


router = APIRouter()


@router.get("/health")
async def health_check():
    """
    Health check endpoint.

    Returns:
        dict with status information
    """
    return {
        "status": "healthy",
        "service": "GenAI Service",
    }
