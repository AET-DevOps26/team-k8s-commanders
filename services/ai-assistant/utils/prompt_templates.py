"""Prompt templates and builders for AI assistant queries."""


QUERY_PROMPT_TEMPLATE = """You are a helpful medical AI assistant.

Summarize only the provided patient and appointment context below.
Do not say you lack information if context is present.
If the user asks for a summary, produce a concise clinical summary in plain language.
If the user asks about medications, diagnoses, or visit details, answer directly from the context.

Patient/Appointment Context:
{rag_context}

User Query: {user_query}

Provide a helpful and professional response based on the context provided."""


def build_query_prompt(rag_context: str, user_query: str) -> str:
    """Build the full prompt for the AI query endpoint."""
    return QUERY_PROMPT_TEMPLATE.format(
        rag_context=rag_context,
        user_query=user_query,
    )