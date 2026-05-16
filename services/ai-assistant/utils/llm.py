"""LLM provider configuration and interface."""

import os
from abc import ABC, abstractmethod
from enum import Enum
from functools import lru_cache
from openai import AsyncOpenAI


class LLMProvider(str, Enum):
    """Supported LLM providers."""
    OPENAI = "openai"
    OPENWEBUI = "openwebui"


class LLMConfig:
    """Configuration for LLM providers."""

    def __init__(self):
        self.provider = os.getenv("LLM_PROVIDER", "openai").lower()

        if self.provider == LLMProvider.OPENAI.value:
            self.openai_api_key = os.getenv("OPENAI_API_KEY")
            self.model_name = os.getenv("OPENAI_MODEL", "gpt-4")
            if not self.openai_api_key:
                raise ValueError("OPENAI_API_KEY not set")

        elif self.provider == LLMProvider.OPENWEBUI.value:
            self.openwebui_base_url = os.getenv("OPENWEBUI_BASE_URL")
            self.openwebui_api_key = os.getenv("OPENWEBUI_API_KEY")
            self.model_name = os.getenv("OPENWEBUI_MODEL", "ollama")
            if not self.openwebui_base_url:
                raise ValueError("OPENWEBUI_BASE_URL not set")
            if not self.openwebui_api_key:
                raise ValueError("OPENWEBUI_API_KEY not set")

        else:
            raise ValueError(f"Unknown LLM provider: {self.provider}")


class LLMInterface(ABC):
    """Abstract interface for LLM providers."""

    @abstractmethod
    async def generate(self, prompt: str) -> str:
        """Generate response from LLM."""
        pass


class OpenAIProvider(LLMInterface):
    """OpenAI provider implementation."""

    def __init__(self, api_key: str, model: str = "gpt-4"):
        self.client = AsyncOpenAI(api_key=api_key)
        self.model = model

    async def generate(self, prompt: str) -> str:
        """Generate response using OpenAI."""
        response = await self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
        )
        return response.choices[0].message.content or ""


class OpenWebUIProvider(LLMInterface):
    """OpenWebUI provider — uses the OpenAI-compatible chat completions endpoint."""

    def __init__(self, endpoint_url: str, api_key: str, model: str = "ollama"):
        self.client = AsyncOpenAI(api_key=api_key, base_url=endpoint_url)
        self.model = model

    async def generate(self, prompt: str) -> str:
        """Generate response using OpenWebUI."""
        response = await self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
        )
        return response.choices[0].message.content or ""


@lru_cache(maxsize=1)
def _get_llm_provider_cached() -> LLMInterface:
    """Cached factory function to get configured LLM provider.
    
    Uses lru_cache to ensure the provider is instantiated once at startup
    and reused across all requests. This preserves connection pooling for
    AsyncOpenAI and avoids redundant environment variable reads.
    """
    config = LLMConfig()

    if config.provider == LLMProvider.OPENAI.value:
        return OpenAIProvider(
            api_key=config.openai_api_key,
            model=config.model_name,
        )

    elif config.provider == LLMProvider.OPENWEBUI.value:
        return OpenWebUIProvider(
            endpoint_url=config.openwebui_base_url,
            api_key=config.openwebui_api_key,
            model=config.model_name,
        )

    else:
        raise ValueError(f"Unknown provider: {config.provider}")


def initialize_llm_provider() -> None:
    """Eagerly initialize the LLM provider at application startup.
    
    This should be called in FastAPI's startup event to ensure the provider
    is created once and cached before any requests are processed.
    """
    _get_llm_provider_cached()


def get_llm_provider() -> LLMInterface:
    """Get the cached LLM provider instance.
    
    Returns the singleton provider that was initialized at startup.
    """
    return _get_llm_provider_cached()
