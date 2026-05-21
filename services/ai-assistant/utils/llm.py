"""LLM configuration — returns a cached LangChain ChatOpenAI instance."""

import os
from functools import lru_cache
from langchain_openai import ChatOpenAI


@lru_cache(maxsize=1)
def get_llm() -> ChatOpenAI:
    provider = os.getenv("LLM_PROVIDER", "openai").lower()
    api_key = os.getenv("LLM_API_KEY")
    model = os.getenv("LLM_MODEL", "gpt-4")

    if not api_key:
        raise ValueError("LLM_API_KEY not set")

    if provider == "openai":
        return ChatOpenAI(api_key=api_key, model=model)

    if provider == "openwebui":
        base_url = os.getenv("OPENWEBUI_BASE_URL")
        if not base_url:
            raise ValueError(
                "OPENWEBUI_BASE_URL not set (required for openwebui provider)"
            )
        return ChatOpenAI(api_key=api_key, base_url=base_url, model=model)

    raise ValueError(f"Unknown LLM provider: {provider}")
