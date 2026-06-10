"""AI Query endpoint implementation."""

import json
import logging

import httpx
from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.responses import StreamingResponse
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

# Media type a client sends in `Accept` to opt into a token-by-token stream.
_SSE_MEDIA_TYPE = "text/event-stream"


def _format_docs(docs) -> str:
    return "\n".join(doc.page_content for doc in docs)


def _sources(docs) -> list[str]:
    # Preserve order, drop duplicates (e.g. several "Clinical note" docs).
    return list(dict.fromkeys(doc.metadata["source"] for doc in docs))


def _sse(event: str, data) -> str:
    """Encode a single Server-Sent Event. ``data`` is JSON so multi-line tokens
    don't break the line-oriented SSE framing."""
    return f"event: {event}\ndata: {json.dumps(data)}\n\n"


def _build_chain(docs):
    """Build the LangChain pipeline and its input for the given context.

    Returns ``(chain, payload)``. Calls ``get_llm()`` eagerly so a misconfigured
    LLM raises here — before any streaming response has started — and can still
    surface as a proper HTTP error.
    """
    llm = get_llm()
    if docs:
        return QUERY_PROMPT | llm | StrOutputParser(), {
            "context": _format_docs(docs),
            "question": "__question__",
        }
    return GENERAL_PROMPT | llm | StrOutputParser(), {"question": "__question__"}


@router.post("/query")
async def query(
    request: AIQueryRequest,
    http_request: Request,
    _role: UserRole = Depends(require_roles(UserRole.DOCTOR, UserRole.ADMIN)),
):
    """Query the AI Assistant, optionally grounded in live patient/appointment context.

    Restricted to DOCTOR and ADMIN roles, authorized from the gateway-provided
    ``X-User-Role`` header. When a patient and/or appointment id is supplied, the
    answer is grounded in data fetched from patient-service and notes-service
    (forwarding the caller's gateway identity headers); supplying an id that
    resolves to nothing is a 404. When no id is supplied, the assistant answers as
    a general-purpose medical reference with no patient context.

    Responds with a single ``AIQueryResponse`` JSON object by default. When the
    client sends ``Accept: text/event-stream`` the answer is streamed token by
    token as Server-Sent Events: a ``sources`` event first (known up front), then
    ``token`` events, then a terminating ``done`` event (or ``error`` on failure).
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

    sources = _sources(docs)

    # Build the chain up front so an LLM configuration error (ValueError from
    # get_llm) becomes a 500 here, before we commit to a 200 streaming response.
    try:
        chain, payload = _build_chain(docs)
    except ValueError as e:
        logger.error("LLM configuration error: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LLM configuration error",
        )
    payload["question"] = request.query

    if _wants_stream(http_request):
        return StreamingResponse(
            _stream_answer(chain, payload, sources),
            media_type=_SSE_MEDIA_TYPE,
            # Defeat proxy/browser response buffering so tokens reach the client
            # as they are produced rather than in one flush at the end.
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    try:
        answer = await chain.ainvoke(payload)
        return AIQueryResponse(
            answer=answer,
            sources=sources,
            confidence=None,  # TODO: compute from LLM output
        )
    except Exception as e:
        logger.exception("Unexpected error processing query: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing query",
        )


def _wants_stream(http_request: Request) -> bool:
    return _SSE_MEDIA_TYPE in http_request.headers.get("accept", "")


async def _stream_answer(chain, payload, sources):
    """Yield the answer as Server-Sent Events.

    The response status is already 200 by the time this runs, so a mid-stream
    failure can't change it — it is reported as a trailing ``error`` event.
    """
    yield _sse("sources", sources)
    try:
        async for chunk in chain.astream(payload):
            if chunk:
                yield _sse("token", chunk)
    except Exception as e:
        logger.exception("Unexpected error streaming query: %s", e)
        yield _sse("error", {"detail": "Error processing query"})
        return
    yield _sse("done", {})
