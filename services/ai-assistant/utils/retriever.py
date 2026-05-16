"""RAG retriever for patient and appointment context.

To connect a real database, replace MockPatientRetriever with a
VectorStoreRetriever (e.g. Chroma, pgvector) or a custom BaseRetriever
that queries your patient service. The retriever contract —
_get_relevant_documents returning a list[Document] — stays the same;
only this class changes.
"""

from typing import Optional
from langchain_core.callbacks.manager import CallbackManagerForRetrieverRun
from langchain_core.documents import Document
from langchain_core.retrievers import BaseRetriever

from utils.mock_data import get_appointment_data, get_patient_complete_history


class MockPatientRetriever(BaseRetriever):
    """Fetches patient/appointment context from mock data.

    TODO: swap for a real retriever once the patient service DB is available.
    """

    patient_id: Optional[str] = None
    appointment_id: Optional[str] = None

    def _get_relevant_documents(
        self,
        _query: str,
        *,
        run_manager: CallbackManagerForRetrieverRun,
    ) -> list[Document]:
        docs: list[Document] = []

        if self.patient_id:
            history = get_patient_complete_history(self.patient_id)
            if history:
                patient = history.get("patient", {})
                if patient:
                    docs.append(Document(
                        page_content=(
                            f"Patient: {patient.get('name')} (DOB: {patient.get('dateOfBirth')})\n"
                            f"Medical History: {', '.join(patient.get('medicalHistory', []))}\n"
                            f"Current Medications: {', '.join(patient.get('currentMedications', []))}"
                        ),
                        metadata={"source": "Patient record"},
                    ))

                for note in history.get("clinical_notes", []):
                    content = f"Clinical Note: {note.get('content')}"
                    if note.get("diagnosis"):
                        content += (
                            f"\nDiagnosis: {note['diagnosis'].get('description')}"
                            f" (Code: {note['diagnosis'].get('code')})"
                        )
                    docs.append(Document(
                        page_content=content,
                        metadata={"source": "Clinical note"},
                    ))

        if self.appointment_id:
            appointment = get_appointment_data(self.appointment_id)
            if appointment:
                docs.append(Document(
                    page_content=(
                        f"Appointment: {appointment.get('reason')}\n"
                        f"Status: {appointment.get('status')}\n"
                        f"Date: {appointment.get('dateTime')}"
                    ),
                    metadata={"source": "Appointment record"},
                ))

        return docs
