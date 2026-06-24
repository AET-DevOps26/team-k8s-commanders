"""Shared test fixtures.

Each test gets its own throwaway SQLite database file (via ``DATABASE_URL``) so
the persistence layer is exercised for real without standing up Postgres. The
cached engine globals are reset around every test so a fresh engine is built in
the event loop that ``TestClient`` runs the app in.
"""

import pytest


@pytest.fixture(autouse=True)
def fresh_db(tmp_path, monkeypatch):
    monkeypatch.setenv("DATABASE_URL", f"sqlite+aiosqlite:///{tmp_path / 'sessions.db'}")

    import db.engine as engine

    engine._engine = None
    engine._sessionmaker = None
    yield
    engine._engine = None
    engine._sessionmaker = None
