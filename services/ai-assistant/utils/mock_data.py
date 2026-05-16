"""Mock patient and appointment data for RAG integration."""

from datetime import datetime, timedelta
from typing import Optional, Dict, List


# Mock patient data - keyed by patientId
MOCK_PATIENTS: Dict[str, dict] = {
    "550e8400-e29b-41d4-a716-446655440000": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "John Doe",
        "email": "john.doe@example.com",
        "role": "PATIENT",
        "dateOfBirth": "1985-05-15",
        "phoneNumber": "+1-555-0101",
        "medicalHistory": [
            "Type 2 Diabetes (diagnosed 2015)",
            "Hypertension (diagnosed 2018)",
            "Allergic to Penicillin",
        ],
        "currentMedications": [
            "Metformin 500mg twice daily",
            "Lisinopril 10mg daily",
        ],
    },
    "550e8400-e29b-41d4-a716-446655440001": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Jane Smith",
        "email": "jane.smith@example.com",
        "role": "PATIENT",
        "dateOfBirth": "1990-08-22",
        "phoneNumber": "+1-555-0102",
        "medicalHistory": [
            "Asthma (childhood)",
            "Migraine headaches",
        ],
        "currentMedications": [
            "Albuterol inhaler as needed",
            "Sumatriptan 50mg for migraines",
        ],
    },
}

# Mock appointment data - keyed by appointmentId
MOCK_APPOINTMENTS: Dict[str, dict] = {
    "660e8400-e29b-41d4-a716-446655440000": {
        "id": "660e8400-e29b-41d4-a716-446655440000",
        "patientId": "550e8400-e29b-41d4-a716-446655440000",
        "doctorId": "770e8400-e29b-41d4-a716-446655440000",
        "dateTime": (datetime.now() + timedelta(days=7)).isoformat(),
        "status": "SCHEDULED",
        "duration": 30,
        "reason": "Diabetes check-up",
        "notes": "Regular monthly follow-up for diabetes management",
    },
    "660e8400-e29b-41d4-a716-446655440001": {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "patientId": "550e8400-e29b-41d4-a716-446655440001",
        "doctorId": "770e8400-e29b-41d4-a716-446655440001",
        "dateTime": (datetime.now() + timedelta(days=14)).isoformat(),
        "status": "SCHEDULED",
        "duration": 45,
        "reason": "Migraine evaluation",
        "notes": "Discussion of new migraine management options",
    },
}

# Mock clinical notes - keyed by appointmentId
MOCK_CLINICAL_NOTES: Dict[str, dict] = {
    "660e8400-e29b-41d4-a716-446655440000": {
        "id": "880e8400-e29b-41d4-a716-446655440000",
        "appointmentId": "660e8400-e29b-41d4-a716-446655440000",
        "doctorId": "770e8400-e29b-41d4-a716-446655440000",
        "content": "Patient presents with good blood glucose control. HbA1c is 6.8%, within target range. Blood pressure 128/82 mmHg, acceptable. Refill prescriptions for Metformin and Lisinopril. Advised on diet and exercise.",
        "diagnosis": {
            "code": "E11.9",
            "description": "Type 2 diabetes mellitus without complications",
        },
        "createdAt": (datetime.now() - timedelta(days=30)).isoformat(),
    },
}


def get_patient_data(patient_id: str) -> Optional[dict]:
    """Retrieve mock patient data by ID."""
    return MOCK_PATIENTS.get(patient_id)


def get_appointment_data(appointment_id: str) -> Optional[dict]:
    """Retrieve mock appointment data by ID."""
    return MOCK_APPOINTMENTS.get(appointment_id)


def get_clinical_notes_for_appointment(appointment_id: str) -> Optional[dict]:
    """Retrieve mock clinical notes for an appointment."""
    return MOCK_CLINICAL_NOTES.get(appointment_id)


def get_patient_appointments(patient_id: str) -> List[dict]:
    """Retrieve all mock appointments for a patient."""
    return [
        apt for apt in MOCK_APPOINTMENTS.values()
        if apt["patientId"] == patient_id
    ]


def get_patient_complete_history(patient_id: str) -> dict:
    """Build complete patient history from mock data for RAG."""
    patient = get_patient_data(patient_id)
    if not patient:
        return {}
    
    appointments = get_patient_appointments(patient_id)
    notes = [
        MOCK_CLINICAL_NOTES[apt["id"]]
        for apt in appointments
        if apt["id"] in MOCK_CLINICAL_NOTES
    ]
    
    return {
        "patient": patient,
        "appointments": appointments,
        "clinical_notes": notes,
    }
