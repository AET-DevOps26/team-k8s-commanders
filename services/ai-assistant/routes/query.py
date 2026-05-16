"""AI Query endpoint implementation."""

from typing import Optional
from uuid import UUID
from fastapi import APIRouter, HTTPException, status
from models.ai_query_request import AIQueryRequest
from models.ai_query_response import AIQueryResponse
from utils.prompt_templates import build_query_prompt
from utils.mock_data import get_patient_complete_history, get_appointment_data
from utils.llm import get_llm_provider

router = APIRouter()


def _build_rag_context(
    patient_id: Optional[UUID] = None,
    appointment_id: Optional[UUID] = None,
) -> Optional[str]:
    """Build context from patient and appointment data for RAG. Returns None if no data found."""
    context_parts = []

    if patient_id:
        patient_history = get_patient_complete_history(str(patient_id))
        if patient_history:
            patient = patient_history.get("patient", {})
            if patient:
                context_parts.append(f"Patient: {patient.get('name')} (DOB: {patient.get('dateOfBirth')})")
                context_parts.append(f"Medical History: {', '.join(patient.get('medicalHistory', []))}")
                context_parts.append(f"Current Medications: {', '.join(patient.get('currentMedications', []))}")

            for note in patient_history.get("clinical_notes", []):
                context_parts.append(f"Clinical Note: {note.get('content')}")
                if note.get("diagnosis"):
                    context_parts.append(f"Diagnosis: {note['diagnosis'].get('description')} (Code: {note['diagnosis'].get('code')})")

    if appointment_id:
        appointment = get_appointment_data(str(appointment_id))
        if appointment:
            context_parts.append(f"Appointment: {appointment.get('reason')}")
            context_parts.append(f"Status: {appointment.get('status')}")
            context_parts.append(f"Date: {appointment.get('dateTime')}")

    return "\n".join(context_parts) if context_parts else None


@router.post("/query")
async def query(
    request: AIQueryRequest,
) -> AIQueryResponse:
    """
    Query the AI Assistant with patient history context.
    
    Supports both OpenAI and OpenWebUI/Ollama models.
    
    Args:
        request: AIQueryRequest containing the query and optional patient/appointment IDs
    Returns:
        AIQueryResponse with the AI-generated answer
        
    Raises:
        HTTPException: If LLM generation fails
    """
    rag_context = _build_rag_context(
        patient_id=request.patient_id,
        appointment_id=request.appointment_id,
    )

    if rag_context is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No patient or appointment data found for the supplied IDs",
        )

    try:
        prompt = build_query_prompt(rag_context=rag_context, user_query=request.query)
        llm_provider = get_llm_provider()
        answer = await llm_provider.generate(prompt)

        return AIQueryResponse(
            answer=answer,
            sources=["Patient history", "Clinical notes", "Appointment records"],
            confidence=0.85,
        )

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"LLM configuration error: {str(e)}",
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing query: {str(e)}",
        )