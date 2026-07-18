import pytest

from utils import llm


@pytest.fixture(autouse=True)
def clear_llm_cache():
    llm.get_llm.cache_clear()
    yield
    llm.get_llm.cache_clear()


def test_missing_api_key_is_rejected(monkeypatch):
    monkeypatch.delenv("LLM_API_KEY", raising=False)

    with pytest.raises(ValueError, match="LLM_API_KEY not set"):
        llm.get_llm()


def test_openwebui_requires_base_url(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "openwebui")
    monkeypatch.setenv("LLM_API_KEY", "test-key")
    monkeypatch.delenv("OPENWEBUI_BASE_URL", raising=False)

    with pytest.raises(ValueError, match="OPENWEBUI_BASE_URL not set"):
        llm.get_llm()


def test_unknown_provider_is_rejected(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "unsupported")
    monkeypatch.setenv("LLM_API_KEY", "test-key")

    with pytest.raises(ValueError, match="Unknown LLM provider: unsupported"):
        llm.get_llm()


def test_openai_configuration_uses_selected_model(monkeypatch):
    captured = {}

    class FakeChatOpenAI:
        def __init__(self, **kwargs):
            captured.update(kwargs)

    monkeypatch.setattr(llm, "ChatOpenAI", FakeChatOpenAI)
    monkeypatch.setenv("LLM_PROVIDER", "OPENAI")
    monkeypatch.setenv("LLM_API_KEY", "test-key")
    monkeypatch.setenv("LLM_MODEL", "test-model")

    first = llm.get_llm()
    second = llm.get_llm()

    assert first is second
    assert captured == {"api_key": "test-key", "model": "test-model"}


def test_openwebui_configuration_forwards_base_url(monkeypatch):
    captured = {}

    class FakeChatOpenAI:
        def __init__(self, **kwargs):
            captured.update(kwargs)

    monkeypatch.setattr(llm, "ChatOpenAI", FakeChatOpenAI)
    monkeypatch.setenv("LLM_PROVIDER", "openwebui")
    monkeypatch.setenv("LLM_API_KEY", "test-key")
    monkeypatch.setenv("OPENWEBUI_BASE_URL", "http://openwebui.local/v1")

    llm.get_llm()

    assert captured["base_url"] == "http://openwebui.local/v1"
