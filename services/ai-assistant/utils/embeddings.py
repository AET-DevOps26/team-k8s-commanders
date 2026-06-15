"""Embedding model configuration — mirrors ``utils.llm.get_llm``.

Embeddings power the clinical-guidelines RAG path (see ``utils.guidelines``).
The provider tracks ``LLM_PROVIDER`` so a deployment stays on one stack: with
``openai`` it calls OpenAI's embeddings API, with ``openwebui`` it calls the
OpenAI-compatible ``/v1/embeddings`` endpoint of the local Ollama/OpenWebUI host.

``EMBEDDING_MODEL`` selects the model and therefore the vector dimension
(e.g. ``text-embedding-3-small`` → 1536, ``nomic-embed-text`` → 768). That
dimension is baked into the pgvector column at ingestion time, so the embedding
model must not change under an existing collection without re-ingesting.
"""

import os
from functools import lru_cache

from langchain_openai import OpenAIEmbeddings

# Sensible default per provider so a minimal .env still works.
_DEFAULT_MODEL = {"openai": "text-embedding-3-small", "openwebui": "nomic-embed-text"}


@lru_cache(maxsize=1)
def get_embeddings() -> OpenAIEmbeddings:
    provider = os.getenv("LLM_PROVIDER", "openai").lower()
    api_key = os.getenv("LLM_API_KEY")
    model = os.getenv("EMBEDDING_MODEL") or _DEFAULT_MODEL.get(provider)

    if not api_key:
        raise ValueError("LLM_API_KEY not set")
    if not model:
        raise ValueError(f"EMBEDDING_MODEL not set and no default for provider: {provider}")

    if provider == "openai":
        return OpenAIEmbeddings(api_key=api_key, model=model)

    if provider == "openwebui":
        base_url = os.getenv("OPENWEBUI_BASE_URL")
        if not base_url:
            raise ValueError(
                "OPENWEBUI_BASE_URL not set (required for openwebui provider)"
            )
        # check_embedding_ctx_length=False: the tokeniser-based batching in
        # langchain-openai assumes OpenAI models; disable it so arbitrary local
        # models (e.g. nomic-embed-text) embed without a tiktoken lookup.
        return OpenAIEmbeddings(
            api_key=api_key,
            base_url=base_url,
            model=model,
            check_embedding_ctx_length=False,
        )

    raise ValueError(f"Unknown LLM provider: {provider}")
