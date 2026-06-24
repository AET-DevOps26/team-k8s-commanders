"""Async CRUD helpers for conversation sessions, all scoped by owner.

Every read/mutation takes the owning ``user_id`` and filters on it, so one
user can never reach another's conversation: a lookup for a session the caller
does not own simply returns ``None`` (the route turns that into a 404, which
also avoids leaking whether the id exists).
"""

import uuid

from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from db.orm import ConversationMessage, ConversationSession, _utcnow


async def create_session(
    db: AsyncSession,
    *,
    user_id: uuid.UUID,
    patient_id: uuid.UUID | None,
    appointment_id: uuid.UUID | None,
    title: str | None,
) -> ConversationSession:
    session = ConversationSession(
        user_id=user_id,
        patient_id=patient_id,
        appointment_id=appointment_id,
        title=title,
    )
    db.add(session)
    await db.commit()
    await db.refresh(session)
    return session


async def list_sessions(
    db: AsyncSession, *, user_id: uuid.UUID, offset: int, limit: int
) -> tuple[list[ConversationSession], int]:
    """Return one page of the user's sessions (newest first) and the total count."""
    base = select(ConversationSession).where(ConversationSession.user_id == user_id)
    total = await db.scalar(
        select(func.count()).select_from(base.subquery())
    )
    rows = await db.scalars(
        base.order_by(ConversationSession.updated_at.desc())
        .offset(offset)
        .limit(limit)
    )
    return list(rows), int(total or 0)


async def get_session(
    db: AsyncSession, *, user_id: uuid.UUID, session_id: uuid.UUID
) -> ConversationSession | None:
    """Fetch a session (with messages) owned by ``user_id``, or ``None``."""
    return await db.scalar(
        select(ConversationSession).where(
            ConversationSession.id == session_id,
            ConversationSession.user_id == user_id,
        )
    )


async def delete_session(
    db: AsyncSession, *, user_id: uuid.UUID, session_id: uuid.UUID
) -> bool:
    """Delete a session the caller owns; return whether a row was removed."""
    result = await db.execute(
        delete(ConversationSession).where(
            ConversationSession.id == session_id,
            ConversationSession.user_id == user_id,
        )
    )
    await db.commit()
    return result.rowcount > 0


async def add_message(
    db: AsyncSession,
    *,
    session: ConversationSession,
    role: str,
    content: str,
    sources: list[str] | None = None,
) -> ConversationMessage:
    message = ConversationMessage(
        session_id=session.id, role=role, content=content, sources=sources
    )
    db.add(message)
    # Bump the parent so list ordering reflects recent activity (a child insert
    # alone would not mark the session row dirty).
    session.updated_at = _utcnow()
    db.add(session)
    await db.commit()
    await db.refresh(message)
    return message
