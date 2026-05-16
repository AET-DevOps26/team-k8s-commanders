# AI Query Endpoint Setup

This document explains the implementation of the `/ai/query` endpoint with LLM provider support and mock patient data.

## Implementation Summary

The `/api/v1/ai/query` endpoint has been fully implemented according to the OpenAPI specification with the following features:

### Key Features

✅ **Public Endpoint** - No JWT authentication required
✅ **Dual LLM Support** - Both OpenAI API and OpenWebUI/Ollama instances
✅ **Mock Patient Data** - Sample patients with medical history, medications, and clinical notes
✅ **RAG-Ready Context** - Dynamically builds context from patient/appointment data
✅ **OpenAPI Compliant** - Response matches spec exactly with answer, sources, and confidence

### Project Structure

```
services/ai-assistant/
├── models/
│   ├── __init__.py
│   ├── schemas.py          # Pydantic request/response models
│   └── llm.py              # LLM provider implementations
├── routes/
│   └── query.py            # Main endpoint implementation
├── utils/
│   ├── __init__.py
│   ├── llm.py              # LLM provider configuration
│   └── mock_data.py        # Mock patient and appointment data
├── main.py                 # FastAPI app setup
├── requirements.txt        # Python dependencies
├── .env.example            # Configuration template
└── tests/
    └── test_query.py       # Endpoint tests
```

## Configuration

Create a `.env` file in `services/ai-assistant/` using `.env.example` as a template:

### For OpenAI:
```bash
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-4
```

### For OpenWebUI/Ollama:
```bash
LLM_PROVIDER=openwebui
OPENWEBUI_BASE_URL=http://localhost:8000
OPENWEBUI_API_KEY=your-openwebui-api-key
OPENWEBUI_MODEL=ollama
```

When you point at a hosted OpenWebUI instance, `OPENWEBUI_API_KEY` should be the API token issued by that instance, and `OPENWEBUI_MODEL` should be the exact model ID returned by the models endpoint, not the display label.

## API Usage

### Request Format

```bash
POST /api/v1/ai/query
Content-Type: application/json

{
  "query": "What is the patient's current status?",
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "appointmentId": "660e8400-e29b-41d4-a716-446655440000"
}
```

### Response Format

```json
{
  "answer": "Based on the patient's medical history and recent clinical notes...",
  "sources": ["Patient history", "Clinical notes", "Appointment records"],
  "confidence": 0.85
}
```

### Example with cURL

```bash
# Make the request
curl -X POST http://localhost:8000/ai/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What medications is the patient currently taking?",
    "patientId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

## Mock Data Available

### Sample Patients

1. **John Doe** (ID: `550e8400-e29b-41d4-a716-446655440000`)
   - Medical History: Type 2 Diabetes, Hypertension, Penicillin allergy
   - Current Medications: Metformin, Lisinopril
   - Upcoming Appointment: Diabetes check-up

2. **Jane Smith** (ID: `550e8400-e29b-41d4-a716-446655440001`)
   - Medical History: Childhood asthma, Migraines
   - Current Medications: Albuterol inhaler, Sumatriptan
   - Upcoming Appointment: Migraine evaluation

### Sample Appointments

- Diabetes check-up (7 days from now)
- Migraine evaluation (14 days from now)

### Clinical Notes

Available for both patients with diagnoses and treatment summaries.

## Testing

Run the test suite:

```bash
cd services/ai-assistant
pytest tests/test_query.py -v
```

Test scenarios included:
- ✓ Public access without authentication
- ✓ Query with patient context
- ✓ Query with appointment context
- ✓ Request validation

## Future Database Integration

When the database is available, replace mock data functions in `utils/mock_data.py`:

```python
# Current (mock)
def get_patient_complete_history(patient_id: str) -> dict:
    return MOCK_PATIENTS.get(patient_id)

# Future (database)
def get_patient_complete_history(patient_id: str) -> dict:
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    appointments = db.query(Appointment).filter(Appointment.patient_id == patient_id).all()
    notes = db.query(ClinicalNote).filter(ClinicalNote.patient_id == patient_id).all()
    return {"patient": patient, "appointments": appointments, "clinical_notes": notes}
```

## RAG Enhancement Roadmap

1. **Phase 1** (Current): Mock data with basic context injection
2. **Phase 2**: Database integration for real patient data
3. **Phase 3**: Vector embeddings for clinical document search
4. **Phase 4**: Semantic search across patient history
5. **Phase 5**: Multi-turn conversation with context persistence

## Environment Variables Reference

| Variable | Required | Default | Example |
|----------|----------|---------|---------|
| `LLM_PROVIDER` | Yes | - | `openai` or `openwebui` |
| `OPENAI_API_KEY` | If OpenAI | - | `sk-...` |
| `OPENAI_MODEL` | If OpenAI | `gpt-4` | `gpt-4` or `gpt-3.5-turbo` |
| `OPENWEBUI_BASE_URL` | If OpenWebUI | `http://localhost:8000` | `https://your-hosted-openwebui.example` |
| `OPENWEBUI_API_KEY` | If OpenWebUI | - | `sk-...` |
| `OPENWEBUI_MODEL` | If OpenWebUI | `ollama` | Exact model ID from the models API |

## Dependencies Added

- `langchain-openai==0.1.8` - OpenAI integration
- `langchain-community==0.0.28` - Community LLM providers
- `openai==1.35.3` - OpenAI API client

## Error Handling

- **422 Validation Error**: Invalid request payload
- **500 Internal Server Error**: LLM configuration or processing error

All errors return RFC 9457 Problem Details format when integrated with main API.
