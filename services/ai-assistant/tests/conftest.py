import os

# Provide fake env vars so LLMConfig can initialize during TestClient lifespan
# without a real .env file. load_dotenv() in main.py won't override these since
# it defaults to override=False.
os.environ.setdefault("LLM_PROVIDER", "openai")
os.environ.setdefault("OPENAI_API_KEY", "sk-fake-key-for-testing")
