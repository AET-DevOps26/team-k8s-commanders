"""Prompt templates for AI assistant queries."""

from langchain_core.prompts import ChatPromptTemplate

QUERY_PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            (
                "You are a helpful medical AI assistant.\n\n"
                "Summarize only the provided patient and appointment context below.\n"
                "Do not say you lack information if context is present.\n"
                "If the user asks for a summary, produce a concise clinical summary in plain language.\n"
                "If the user asks about medications, diagnoses, or visit details, answer directly from the context.\n\n"
                "Patient/Appointment Context:\n{context}"
            ),
        ),
        ("human", "{question}"),
    ]
)
