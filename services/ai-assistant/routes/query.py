"""AI Query endpoint implementation."""

import logging

import httpx
from fastapi import APIRouter, Depends, HTTPException, Request, status
from langchain_core.output_parsers import StrOutputParser

from models.ai_query_request import AIQueryRequest
from models.ai_query_response import AIQueryResponse
from models.user_role import UserRole
from utils.auth import require_roles
from utils.context import build_context
from utils.llm import get_llm
from utils.prompt_templates import GENERAL_PROMPT, QUERY_PROMPT
from utils.service_client import forwarded_headers

logger = logging.getLogger(__name__)

router = APIRouter()


def _format_docs(docs) -> str:
    return "\n".join(doc.page_content for doc in docs)


@router.post("/query")
async def query(
    request: AIQueryRequest,
    http_request: Request,
    _role: UserRole = Depends(require_roles(UserRole.DOCTOR, UserRole.ADMIN)),
) -> AIQueryResponse:
    """Query the AI Assistant, optionally grounded in live patient/appointment context.

    Restricted to DOCTOR and ADMIN roles, authorized from the gateway-provided
    ``X-User-Role`` header. When a patient and/or appointment id is supplied, the
    answer is grounded in data fetched from patient-service and notes-service
    (forwarding the caller's gateway identity headers); supplying an id that
    resolves to nothing is a 404. When no id is supplied, the assistant answers as
    a general-purpose medical reference with no patient context.
    """
    headers = forwarded_headers(http_request.headers)
    ids_supplied = bool(request.patient_id or request.appointment_id)

    try:
        docs = await build_context(
            patient_id=str(request.patient_id) if request.patient_id else None,
            appointment_id=str(request.appointment_id) if request.appointment_id else None,
            headers=headers,
        )
    except httpx.HTTPError as e:
        logger.error("Failed to fetch patient context from upstream service: %s", e)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to retrieve patient context",
        )

    # An id that resolves to no data is a client error; no id at all is a valid
    # general-knowledge query that runs without grounding context.
    if ids_supplied and not docs:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No patient or appointment data found for the supplied IDs",
        )

    try:
        if docs:
            chain = QUERY_PROMPT | get_llm() | StrOutputParser()
            answer = await chain.ainvoke(
                {
                    "context": _format_docs(docs),
                    "question": request.query,
                }
            )
        else:
            chain = GENERAL_PROMPT | get_llm() | StrOutputParser()
            answer = await chain.ainvoke({"question": request.query})

        return AIQueryResponse(
            answer=answer,
            sources=list(dict.fromkeys(doc.metadata["source"] for doc in docs)),
            confidence=None,  # TODO: compute from LLM output
        )

    except ValueError as e:
        logger.error("LLM configuration error: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LLM configuration error",
        )
    except Exception as e:
        logger.exception("Unexpected error processing query: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing query",
        )
