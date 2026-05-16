"""LLM provider configuration and interface."""

import asyncio
import json
import os
from abc import ABC, abstractmethod
from enum import Enum
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
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
            self.openwebui_chat_completions_url = os.getenv("OPENWEBUI_CHAT_COMPLETIONS_URL")
            self.openwebui_api_key = os.getenv("OPENWEBUI_API_KEY")
            self.model_name = os.getenv("OPENWEBUI_MODEL", "ollama")
            if not self.openwebui_chat_completions_url:
                raise ValueError("OPENWEBUI_CHAT_COMPLETIONS_URL not set")
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


def _post_chat_completion(url: str, api_key: str, model: str, prompt: str) -> str:
    """Send a chat completion request and return the assistant message content."""
    payload = json.dumps(
        {
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.7,
        }
    ).encode("utf-8")

    request = Request(
        url=url,
        data=payload,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urlopen(request, timeout=60) as response:
            response_data = json.loads(response.read().decode("utf-8"))
    except HTTPError as error:
        error_body = error.read().decode("utf-8", errors="replace")
        raise ValueError(f"LLM request failed with HTTP {error.code}: {error_body}") from error
    except URLError as error:
        raise ValueError(f"LLM request failed: {error.reason}") from error

    choices = response_data.get("choices", [])
    if not choices:
        raise ValueError("LLM response did not include any choices")

    message = choices[0].get("message", {})
    content = message.get("content")
    if not content:
        raise ValueError("LLM response did not include assistant content")

    return content


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
    """OpenWebUI provider implementation using the configured chat endpoint."""

    def __init__(self, endpoint_url: str, api_key: str, model: str = "ollama"):
        self.api_url = endpoint_url
        self.api_key = api_key
        self.model = model

    async def generate(self, prompt: str) -> str:
        """Generate response using OpenWebUI."""
        return await asyncio.to_thread(
            _post_chat_completion,
            self.api_url,
            self.api_key,
            self.model,
            prompt,
        )


def get_llm_provider() -> LLMInterface:
    """Factory function to get configured LLM provider."""
    config = LLMConfig()

    if config.provider == LLMProvider.OPENAI.value:
        return OpenAIProvider(
            api_key=config.openai_api_key,
            model=config.model_name,
        )

    elif config.provider == LLMProvider.OPENWEBUI.value:
        return OpenWebUIProvider(
            endpoint_url=config.openwebui_chat_completions_url,
            api_key=config.openwebui_api_key,
            model=config.model_name,
        )

    else:
        raise ValueError(f"Unknown provider: {config.provider}")
