"""Persistent AI conversation sessions.

A session is owned by a user and optionally bound to a patient/appointment at
creation. Each message turn re-fetches that grounding context live (data may
have changed) and replays the whole prior conversation to the model, so the
assistant reasons over the conversation rather than a single isolated question.
Both the user's message and the assistant's reply are persisted.
"""

import json
import logging
import uuid

import httpx
from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from fastapi.responses import StreamingResponse
from langchain_core.messages import AIMessage as LCAIMessage, HumanMessage
from langchain_core.output_parsers import StrOutputParser
from sqlalchemy.ext.asyncio import AsyncSession

from db import repository
from db.engine import get_db
from db.orm import ConversationMessage, ConversationSession
from models.ai_message import AIMessage
from models.ai_message_request import AIMessageRequest
from models.ai_message_response import AIMessageResponse
from models.ai_message_role import AIMessageRole
from models.ai_session import AISession
from models.ai_session_create_request import AISessionCreateRequest
from models.ai_session_summary import AISessionSummary
from models.page_meta import PageMeta
from models.paginated_ai_session_response import PaginatedAISessionResponse
from models.user_role import UserRole
from utils.auth import require_roles, require_user_id
from utils.context import build_context
from utils.guidelines import retrieve_guidelines
from utils.llm import get_llm
from utils.prompt_templates import GENERAL_PROMPT, QUERY_PROMPT
from utils.service_client import forwarded_headers

logger = logging.getLogger(__name__)

router = APIRouter()

# Media type a client sends in `Accept` to opt into a token-by-token stream.
_SSE_MEDIA_TYPE = "text/event-stream"

# Any DOCTOR/ADMIN may use the assistant; ownership is enforced separately by id.
_authz = Depends(require_roles(UserRole.DOCTOR, UserRole.ADMIN))


def _format_docs(docs) -> str:
    return "\n".join(doc.page_content for doc in docs)


def _format_guidelines(docs) -> str:
    """Render retrieved guideline chunks as a labelled block for the prompt.

    Returns "" when nothing was retrieved so the ``{guidelines}`` placeholder in
    the system prompt collapses to nothing.
    """
    if not docs:
        return ""
    body = "\n\n".join(
        f"[{doc.metadata['source']}]\n{doc.page_content}" for doc in docs
    )
    return "\n\nClinical guideline excerpts (general reference):\n" + body


def _sources(docs) -> list[str]:
    # Preserve order, drop duplicates (e.g. several "Clinical note" docs).
    return list(dict.fromkeys(doc.metadata["source"] for doc in docs))


def _sse(event: str, data) -> str:
    """Encode a single Server-Sent Event. ``data`` is JSON so multi-line tokens
    don't break the line-oriented SSE framing."""
    return f"event: {event}\ndata: {json.dumps(data)}\n\n"


def _wants_stream(http_request: Request) -> bool:
    return _SSE_MEDIA_TYPE in http_request.headers.get("accept", "")


def _to_history(messages: list[ConversationMessage]) -> list:
    """Convert stored messages into LangChain chat messages for replay."""
    history = []
    for m in messages:
        if m.role == AIMessageRole.ASSISTANT.value:
            history.append(LCAIMessage(content=m.content))
        else:
            history.append(HumanMessage(content=m.content))
    return history


def _build_chain(docs, guideline_docs, history):
    """Build the LangChain pipeline and its input for the given context.

    Returns ``(chain, payload)``. Calls ``get_llm()`` eagerly so a misconfigured
    LLM raises here — before any streaming response has started — and can still
    surface as a proper HTTP error. ``guideline_docs`` are the RAG hits; both
    prompts carry a ``{guidelines}`` slot, so it is always supplied (empty when
    nothing was retrieved).
    """
    llm = get_llm()
    prompt = QUERY_PROMPT if docs else GENERAL_PROMPT
    payload = {
        "history": history,
        "question": "__question__",
        "guidelines": _format_guidelines(guideline_docs),
    }
    if docs:
        payload["context"] = _format_docs(docs)
    return prompt | llm | StrOutputParser(), payload


def _api_message(m: ConversationMessage) -> AIMessage:
    return AIMessage(
        id=m.id,
        role=AIMessageRole(m.role),
        content=m.content,
        sources=m.sources,
        created_at=m.created_at,
    )


def _api_session(session: ConversationSession) -> AISession:
    return AISession(
        id=session.id,
        user_id=session.user_id,
        patient_id=session.patient_id,
        appointment_id=session.appointment_id,
        title=session.title,
        created_at=session.created_at,
        updated_at=session.updated_at,
        messages=[_api_message(m) for m in session.messages],
    )


def _api_summary(session: ConversationSession) -> AISessionSummary:
    return AISessionSummary(
        id=session.id,
        user_id=session.user_id,
        patient_id=session.patient_id,
        appointment_id=session.appointment_id,
        title=session.title,
        created_at=session.created_at,
        updated_at=session.updated_at,
    )


@router.post("/sessions", status_code=status.HTTP_201_CREATED)
async def create_session(
    request: AISessionCreateRequest,
    db: AsyncSession = Depends(get_db),
    user_id: uuid.UUID = Depends(require_user_id),
    _role: UserRole = _authz,
) -> AISession:
    """Start a new conversation, optionally bound to a patient/appointment."""
    session = await repository.create_session(
        db,
        user_id=user_id,
        patient_id=request.patient_id,
        appointment_id=request.appointment_id,
        title=request.title,
    )
    return _api_session(session)


@router.get("/sessions")
async def list_sessions(
    page: int = Query(default=0, ge=0),
    size: int = Query(default=20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    user_id: uuid.UUID = Depends(require_user_id),
    _role: UserRole = _authz,
) -> PaginatedAISessionResponse:
    """List the caller's sessions, most recently active first."""
    sessions, total = await repository.list_sessions(
        db, user_id=user_id, offset=page * size, limit=size
    )
    return PaginatedAISessionResponse(
        content=[_api_summary(s) for s in sessions],
        page=PageMeta(
            page=page,
            size=size,
            total_elements=total,
            total_pages=(total + size - 1) // size if size else 0,
        ),
    )


async def _load_owned_session(
    db: AsyncSession, user_id: uuid.UUID, session_id: uuid.UUID
) -> ConversationSession:
    session = await repository.get_session(db, user_id=user_id, session_id=session_id)
    if session is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Session not found"
        )
    return session


@router.get("/sessions/{session_id}")
async def get_session(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    user_id: uuid.UUID = Depends(require_user_id),
    _role: UserRole = _authz,
) -> AISession:
    """Return a session with its full message history."""
    session = await _load_owned_session(db, user_id, session_id)
    return _api_session(session)


@router.delete("/sessions/{session_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_session(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    user_id: uuid.UUID = Depends(require_user_id),
    _role: UserRole = _authz,
) -> None:
    """Delete a session and all its messages."""
    deleted = await repository.delete_session(
        db, user_id=user_id, session_id=session_id
    )
    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Session not found"
        )


@router.post("/sessions/{session_id}/messages")
async def create_message(
    session_id: uuid.UUID,
    request: AIMessageRequest,
    http_request: Request,
    db: AsyncSession = Depends(get_db),
    user_id: uuid.UUID = Depends(require_user_id),
    _role: UserRole = _authz,
):
    """Post a user message and return the assistant's grounded, context-aware reply."""
    session = await _load_owned_session(db, user_id, session_id)

    headers = forwarded_headers(http_request.headers)
    ids_supplied = bool(session.patient_id or session.appointment_id)

    try:
        docs = await build_context(
            patient_id=str(session.patient_id) if session.patient_id else None,
            appointment_id=str(session.appointment_id) if session.appointment_id else None,
            headers=headers,
        )
    except httpx.HTTPError as e:
        logger.error("Failed to fetch patient context from upstream service: %s", e)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to retrieve patient context",
        )

    # A bound id that no longer resolves to any data is a client-visible error.
    if ids_supplied and not docs:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No patient or appointment data found for the session's bound IDs",
        )

    # Retrieve relevant clinical guidelines (RAG). Best-effort: a knowledge base
    # that is absent or unreachable yields no docs and never blocks the reply.
    guideline_docs = await retrieve_guidelines(request.query)

    # Guideline labels are surfaced alongside patient/appointment/note sources.
    sources = _sources(docs + guideline_docs)
    history = _to_history(session.messages)

    # Build the chain up front so an LLM configuration error becomes a 500 here,
    # before we persist anything or commit to a 200 streaming response.
    try:
        chain, payload = _build_chain(docs, guideline_docs, history)
    except ValueError as e:
        logger.error("LLM configuration error: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LLM configuration error",
        )
    payload["question"] = request.query

    if _wants_stream(http_request):
        return StreamingResponse(
            _stream_answer(chain, payload, sources, db, session, request.query),
            media_type=_SSE_MEDIA_TYPE,
            # Defeat proxy/browser response buffering so tokens reach the client
            # as they are produced rather than in one flush at the end.
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    try:
        answer = await chain.ainvoke(payload)
    except Exception as e:
        logger.exception("Unexpected error processing query: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing query",
        )

    await repository.add_message(
        db, session=session, role=AIMessageRole.USER.value, content=request.query
    )
    await repository.add_message(
        db,
        session=session,
        role=AIMessageRole.ASSISTANT.value,
        content=answer,
        sources=sources or None,
    )
    return AIMessageResponse(answer=answer, sources=sources, confidence=None)


async def _stream_answer(chain, payload, sources, db, session, user_query: str):
    """Yield the answer as Server-Sent Events and persist it on completion.

    The response status is already 200 by the time this runs, so a mid-stream
    failure can't change it — it is reported as a trailing ``error`` event and
    the assistant reply is not persisted.
    """
    yield _sse("sources", sources)
    chunks: list[str] = []
    try:
        async for chunk in chain.astream(payload):
            if chunk:
                chunks.append(chunk)
                yield _sse("token", chunk)
    except Exception as e:
        logger.exception("Unexpected error streaming query: %s", e)
        yield _sse("error", {"detail": "Error processing query"})
        return
    await repository.add_message(
        db,
        session=session,
        role=AIMessageRole.USER.value,
        content=user_query,
    )
    await repository.add_message(
        db,
        session=session,
        role=AIMessageRole.ASSISTANT.value,
        content="".join(chunks),
        sources=sources or None,
    )
    yield _sse("done", {})
