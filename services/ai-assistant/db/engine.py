"""Async SQLAlchemy engine, session factory, and FastAPI dependency.

The DSN is taken from ``DATABASE_URL`` if set, otherwise assembled from the
``DB_*`` env vars (matching the per-service Postgres convention used across the
platform). The engine is created lazily so importing this module never opens a
connection — handy for tests, which point ``DATABASE_URL`` at SQLite.
"""

import os
from collections.abc import AsyncIterator
from urllib.parse import quote_plus

from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.pool import StaticPool

from db.orm import Base

_engine: AsyncEngine | None = None
_sessionmaker: async_sessionmaker[AsyncSession] | None = None


def database_url() -> str:
    url = os.getenv("DATABASE_URL")
    if url:
        return url
    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "5432")
    name = os.getenv("DB_NAME", "ai_db")
    user = os.getenv("DB_USER", "caredesk")
    password = os.getenv("DB_PASSWORD", "caredesk")
    return (
        f"postgresql+asyncpg://{quote_plus(user)}:{quote_plus(password)}"
        f"@{host}:{port}/{name}"
    )


def get_engine() -> AsyncEngine:
    global _engine, _sessionmaker
    if _engine is None:
        url = database_url()
        # In-memory SQLite (tests) lives in a single connection, so pin the pool
        # to one shared connection; otherwise each session would see an empty DB.
        if url.startswith("sqlite") and ":memory:" in url:
            _engine = create_async_engine(
                url, connect_args={"check_same_thread": False}, poolclass=StaticPool
            )
        else:
            _engine = create_async_engine(url, pool_pre_ping=True)
        _sessionmaker = async_sessionmaker(_engine, expire_on_commit=False)
    return _engine


def get_sessionmaker() -> async_sessionmaker[AsyncSession]:
    if _sessionmaker is None:
        get_engine()
    assert _sessionmaker is not None
    return _sessionmaker


async def init_models() -> None:
    """Create tables that don't yet exist (dev convenience; prod uses migrations)."""
    engine = get_engine()
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


async def get_db() -> AsyncIterator[AsyncSession]:
    """FastAPI dependency yielding a request-scoped async session."""
    async with get_sessionmaker()() as session:
        yield session
