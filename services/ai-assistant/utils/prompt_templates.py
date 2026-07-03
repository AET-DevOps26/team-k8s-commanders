"""Prompt templates for AI assistant queries.

The session endpoints replay the whole conversation, so the templates carry a
``MessagesPlaceholder("history")`` between the system instructions and the new
question. The grounded vs. general split is the same as before: when a session
is bound to a patient/appointment the system message embeds the live context,
otherwise the assistant answers as a general medical reference.
"""

from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

_GROUNDED_SYSTEM = (
    "You are a helpful medical AI assistant.\n\n"
    "Summarize only the provided patient and appointment context below.\n"
    "Do not say you lack information if context is present.\n"
    "If the user asks for a summary, produce a concise clinical summary in plain language.\n"
    "If the user asks about medications, diagnoses, or visit details, answer directly from the context.\n\n"
    "Patient/Appointment Context:\n{context}"
)

# Used when the session is not bound to a patient/appointment: there is no record
# to ground in, so the assistant answers as a general-purpose medical reference for
# the clinician. It must not invent patient-specific facts.
_GENERAL_SYSTEM = (
    "You are a helpful medical AI assistant supporting a clinician.\n\n"
    "No patient or appointment record was supplied, so answer the question "
    "as general medical information (e.g. conditions, medications, guidelines, "
    "definitions).\n"
    "Do not invent details about any specific patient; you have none.\n"
    "If answering well would require patient-specific data, tell the user you "
    "have no patient context in this conversation and ask them to open a new "
    "conversation from the patient's page, which loads that patient's record "
    "so you can give a grounded answer."
)

QUERY_PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", _GROUNDED_SYSTEM),
        MessagesPlaceholder("history"),
        ("human", "{question}"),
    ]
)

GENERAL_PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", _GENERAL_SYSTEM),
        MessagesPlaceholder("history"),
        ("human", "{question}"),
    ]
)
