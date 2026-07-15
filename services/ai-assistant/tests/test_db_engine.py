from db import engine


def test_database_url_prefers_explicit_url(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "sqlite+aiosqlite:///explicit.db")

    assert engine._database_url() == "sqlite+aiosqlite:///explicit.db"


def test_database_url_encodes_credentials(monkeypatch):
    monkeypatch.delenv("DATABASE_URL", raising=False)
    monkeypatch.setenv("DB_HOST", "postgres.internal")
    monkeypatch.setenv("DB_PORT", "5433")
    monkeypatch.setenv("DB_NAME", "care desk")
    monkeypatch.setenv("DB_USER", "care@desk")
    monkeypatch.setenv("DB_PASSWORD", "p@ss/word")

    assert engine._database_url() == (
        "postgresql+asyncpg://care%40desk:p%40ss%2Fword"
        "@postgres.internal:5433/care desk"
    )
