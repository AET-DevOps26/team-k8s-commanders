"""Shared test fixtures.

Each test gets its own throwaway SQLite database file (via ``DATABASE_URL``) so
the persistence layer is exercised for real without standing up Postgres. The
cached engine globals are reset around every test so a fresh engine is built in
the event loop that ``TestClient`` runs the app in.
"""

import pytest
from opentelemetry import trace


def pytest_sessionfinish(session, exitstatus):
    # Stops the BatchSpanProcessor's background export thread before pytest
    # tears down its log-capture streams. Without this, the thread's next
    # scheduled export (Tempo is never reachable in unit tests) logs a
    # connection-refused warning after stdout is already closed, printing a
    # spurious "I/O operation on closed file" traceback per test run.
    trace.get_tracer_provider().shutdown()


@pytest.fixture(autouse=True)
def fresh_db(tmp_path, monkeypatch):
    monkeypatch.setenv("DATABASE_URL", f"sqlite+aiosqlite:///{tmp_path / 'sessions.db'}")

    import db.engine as engine

    engine._engine = None
    engine._sessionmaker = None
    yield
    engine._engine = None
    engine._sessionmaker = None
