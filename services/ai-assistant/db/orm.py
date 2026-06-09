"""SQLAlchemy ORM entities for persisted AI conversations.

A ``ConversationSession`` is owned by a single user (the gateway-supplied
``X-User-Id``) and may be bound to one patient and/or appointment; that binding
is fixed for the session's lifetime and grounds every turn. Each turn appends
two ``ConversationMessage`` rows (the user's question and the assistant's reply)
so the whole conversation can be replayed back to the model.

These live in their own package rather than ``models/`` because that directory
is wiped and regenerated from the OpenAPI spec by ``api/scripts/gen-all.sh``.
"""

import uuid
from datetime import datetime, timezone

from sqlalchemy import DateTime, ForeignKey, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship
from sqlalchemy.types import JSON
from sqlalchemy import Uuid


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


class Base(DeclarativeBase):
    pass


class ConversationSession(Base):
    __tablename__ = "conversation_sessions"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    # Owner of the conversation; all access is scoped to this id.
    user_id: Mapped[uuid.UUID] = mapped_column(Uuid, index=True, nullable=False)
    patient_id: Mapped[uuid.UUID | None] = mapped_column(Uuid, nullable=True)
    appointment_id: Mapped[uuid.UUID | None] = mapped_column(Uuid, nullable=True)
    title: Mapped[str | None] = mapped_column(String(255), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )

    messages: Mapped[list["ConversationMessage"]] = relationship(
        back_populates="session",
        cascade="all, delete-orphan",
        order_by="ConversationMessage.created_at",
        lazy="selectin",
    )


class ConversationMessage(Base):
    __tablename__ = "conversation_messages"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    session_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("conversation_sessions.id", ondelete="CASCADE"),
        index=True,
        nullable=False,
    )
    # "user" or "assistant" — matches the AIMessageRole enum in the OpenAPI spec.
    role: Mapped[str] = mapped_column(String(16), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    # Grounding sources cited for an assistant reply; null for user messages.
    sources: Mapped[list[str] | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, nullable=False
    )

    session: Mapped[ConversationSession] = relationship(back_populates="messages")
