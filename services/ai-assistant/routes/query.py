"""AI Query endpoint implementation."""

from fastapi import APIRouter, HTTPException, status
from langchain_core.output_parsers import StrOutputParser

from models.ai_query_request import AIQueryRequest
from models.ai_query_response import AIQueryResponse
from utils.llm import get_llm
from utils.prompt_templates import QUERY_PROMPT
from utils.retriever import MockPatientRetriever

router = APIRouter()


def _format_docs(docs) -> str:
    return "\n".join(doc.page_content for doc in docs)


@router.post("/query")
async def query(request: AIQueryRequest) -> AIQueryResponse:
    """Query the AI Assistant with patient/appointment context."""
    retriever = MockPatientRetriever(
        patient_id=str(request.patient_id) if request.patient_id else None,
        appointment_id=str(request.appointment_id) if request.appointment_id else None,
    )

    docs = await retriever.ainvoke(request.query)
    if not docs:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No patient or appointment data found for the supplied IDs",
        )

    try:
        chain = QUERY_PROMPT | get_llm() | StrOutputParser()
        answer = await chain.ainvoke({
            "context": _format_docs(docs),
            "question": request.query,
        })

        return AIQueryResponse(
            answer=answer,
            sources=list(dict.fromkeys(doc.metadata["source"] for doc in docs)),
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
