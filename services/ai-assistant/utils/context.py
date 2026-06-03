"""Builds grounded LLM context from live backend data.

Patient data is the source of truth, so the assistant injects it directly:
demographics and visit history (appointments + clinical notes) from
patient-service, plus per-appointment details and the note from notes-service.
No vector store is involved for patient data — semantic retrieval is reserved
for the medical-guidelines knowledge base (future work).

Each fact becomes a LangChain ``Document`` whose ``metadata["source"]`` label
is surfaced back to the caller in ``AIQueryResponse.sources``.
"""

from langchain_core.documents import Document

from utils import service_client

SOURCE_PATIENT = "Patient record"
SOURCE_APPOINTMENT = "Appointment record"
SOURCE_NOTE = "Clinical note"


def _format_profile(profile: dict) -> str:
    parts = [f"Patient: {profile.get('name')}"]
    if profile.get("dateOfBirth"):
        parts.append(f"Date of birth: {profile['dateOfBirth']}")
    if profile.get("phoneNumber"):
        parts.append(f"Phone: {profile['phoneNumber']}")
    return "\n".join(parts)


def _format_appointment(appointment: dict) -> str:
    reason = appointment.get("reason") or "no reason recorded"
    lines = [f"Appointment ({appointment.get('dateTime')}): {reason}"]
    status = appointment.get("status")
    duration = appointment.get("duration")
    if status or duration is not None:
        lines.append(f"Status: {status}, Duration: {duration} min")
    return "\n".join(lines)


def _format_note(note: dict) -> str:
    content = f"Clinical note: {note.get('content')}"
    diagnosis = note.get("diagnosis")
    if diagnosis:
        content += (
            f"\nDiagnosis: {diagnosis.get('description')} (Code: {diagnosis.get('code')})"
        )
    return content


async def build_context(
    patient_id: str | None,
    appointment_id: str | None,
    headers: dict,
) -> list[Document]:
    """Assemble grounding documents for the supplied patient/appointment ids.

    Returns an empty list when neither id resolves to any data, which the route
    turns into a 404.
    """
    docs: list[Document] = []

    if patient_id:
        profile = await service_client.get_patient_profile(patient_id, headers)
        if profile:
            docs.append(Document(page_content=_format_profile(profile),
                                 metadata={"source": SOURCE_PATIENT}))

        history = await service_client.get_visit_history(patient_id, headers)
        if history:
            # Most recent visits first so the model leads with current context.
            appointments = sorted(
                history.get("appointments") or [],
                key=lambda a: a.get("dateTime") or "",
                reverse=True,
            )
            for appointment in appointments:
                docs.append(Document(page_content=_format_appointment(appointment),
                                     metadata={"source": SOURCE_APPOINTMENT}))
            for note in history.get("notes") or []:
                docs.append(Document(page_content=_format_note(note),
                                     metadata={"source": SOURCE_NOTE}))

    if appointment_id:
        appointment = await service_client.get_appointment(appointment_id, headers)
        if appointment:
            docs.append(Document(page_content=_format_appointment(appointment),
                                 metadata={"source": SOURCE_APPOINTMENT}))

        note = await service_client.get_appointment_note(appointment_id, headers)
        if note:
            docs.append(Document(page_content=_format_note(note),
                                 metadata={"source": SOURCE_NOTE}))

    return docs
