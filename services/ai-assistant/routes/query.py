"""AI Query endpoint implementation."""

from typing import Optional
from fastapi import APIRouter, HTTPException, status
from models.ai_query_request import AIQueryRequest
from models.ai_query_response import AIQueryResponse
from utils.prompt_templates import build_query_prompt
from utils.mock_data import get_patient_complete_history, get_appointment_data
from utils.llm import get_llm_provider

router = APIRouter()


def _build_rag_context(
    patient_id: Optional[str] = None,
    appointment_id: Optional[str] = None,
) -> str:
    """Build context from patient and appointment data for RAG."""
    context_parts = []

    patient_id_str = str(patient_id) if patient_id is not None else None
    appointment_id_str = str(appointment_id) if appointment_id is not None else None
    
    if patient_id_str:
        patient_history = get_patient_complete_history(patient_id_str)
        if patient_history:
            patient = patient_history.get("patient", {})
            if patient:
                context_parts.append(f"Patient: {patient.get('name')} (DOB: {patient.get('dateOfBirth')})")
                context_parts.append(f"Medical History: {', '.join(patient.get('medicalHistory', []))}")
                context_parts.append(f"Current Medications: {', '.join(patient.get('currentMedications', []))}")
            
            # Add recent appointments and notes
            for note in patient_history.get("clinical_notes", []):
                context_parts.append(f"Clinical Note: {note.get('content')}")
                if note.get("diagnosis"):
                    context_parts.append(f"Diagnosis: {note['diagnosis'].get('description')} (Code: {note['diagnosis'].get('code')})")
    
    if appointment_id_str:
        appointment = get_appointment_data(appointment_id_str)
        if appointment:
            context_parts.append(f"Appointment: {appointment.get('reason')}")
            context_parts.append(f"Status: {appointment.get('status')}")
            context_parts.append(f"Date: {appointment.get('dateTime')}")
    
    return "\n".join(context_parts) if context_parts else "No patient or appointment context available."


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
    try:
        # Build RAG context from mock data
        rag_context = _build_rag_context(
            patient_id=request.patient_id,
            appointment_id=request.appointment_id,
        )
        
        if rag_context == "No patient or appointment context available.":
            raise ValueError("No patient or appointment data found for the supplied IDs")

        prompt = build_query_prompt(rag_context=rag_context, user_query=request.query)
        
        # Get configured LLM provider and generate response
        llm_provider = get_llm_provider()
        answer = await llm_provider.generate(prompt)
        
        # Return response matching OpenAPI spec
        return AIQueryResponse(
            answer=answer,
            sources=["Patient history", "Clinical notes", "Appointment records"] if rag_context else [],
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