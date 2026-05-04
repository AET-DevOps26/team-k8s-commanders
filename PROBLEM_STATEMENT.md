# 📝 Problem Statement

---

## 1. Problem Statement

Clinics and outpatient practices rely on heavily fragmented, outdated software to manage their day-to-day operations. Appointment scheduling, patient records, clinical documentation, and follow-up reminders are handled by separate disconnected tools — or in many cases, still done manually via phone calls, paper forms, and spreadsheets.

The consequences are tangible: doctors spend over 3 hours per day on administrative tasks rather than patient care, appointments are missed due to lack of automated reminders, and patient history is scattered across systems with no unified view. This is not a niche problem — Munich-based startup Avelios Medical was founded specifically to solve it and recently raised €30 million from Sequoia Capital.

**CareDesk** addresses this by providing a unified, modern clinic management platform built on a microservices architecture, covering the core workflows of an outpatient clinic in one coherent system.

---

## 2. Main Functionality

CareDesk covers four core functional areas:

- **Authentication & Role Management** — Secure registration and login for three user roles: Patient, Doctor, and Admin. Each role gets a tailored dashboard and access permissions.
- **Appointment Booking** — Patients can view doctor availability and book, reschedule, or cancel appointments. Doctors can manage their schedules and view upcoming consultations.
- **Clinical Notes** — Doctors can write structured visit notes tied to a patient's profile, including diagnosis tags and treatment summaries. Patients can view a read-only history of their visits.
- **Automated Notifications** — Appointment confirmation and reminder emails (and optionally SMS) are sent automatically to reduce no-shows.

---

## 3. Intended Users

| User | Role in the System |
|---|---|
| **Patients** | Register, book appointments, view visit history and upcoming schedule |
| **Doctors** | Manage schedule, write clinical notes, query patient history via AI assistant |
| **Clinic Admin** | Oversee the platform, manage user accounts, monitor clinic activity |

---

## 4. GenAI Integration

The AI component is a **RAG-based clinical assistant** embedded in the Doctor Dashboard. It is powered by a Large Language Model grounded in two data sources:

1. **Live patient data** — structured records from the Clinical Notes and Appointments services (visit history, diagnoses, follow-up status)
2. **Medical guidelines knowledge base** — a curated set of documents from sources such as WHO and NHS clinical guidelines, indexed for retrieval

This means the assistant does not hallucinate freely — every response is grounded in either real patient data or established medical guidelines.

**Example queries a doctor can ask:**
- *"Summarise the last 5 visits for patient Anna Müller"*
- *"Which of my patients are overdue for a follow-up this week?"*
- *"What are the recommended follow-up intervals for a patient diagnosed with hypertension?"*

The integration is meaningful rather than cosmetic: it directly addresses the documented problem of doctor time lost to admin by making patient history instantly queryable in natural language.

---

## 5. Application Scenarios

### Scenario 1 — Patient books an appointment
Maria registers on CareDesk and searches for an available GP slot. She selects a time, books it, and immediately receives a confirmation email. 24 hours before the appointment she gets an automatic reminder. She does not need to call the clinic.

### Scenario 2 — Doctor conducts a consultation
Dr. Schmidt opens his dashboard and sees the day's appointments. Before a consultation he pulls up the patient's visit history. After the consultation he writes a structured note, tags the diagnosis, and marks a follow-up in 4 weeks. The patient's record is updated instantly.

### Scenario 3 — Doctor queries the AI assistant
Dr. Schmidt types: *"Which of my patients were diagnosed with Type 2 diabetes and haven't had a follow-up in over 6 months?"* The assistant queries the patient database and returns a list with names and last visit dates. Dr. Schmidt can act on this immediately without manually searching through records.

### Scenario 4 — Admin manages the clinic
The clinic admin logs in, reviews the week's appointment load, deactivates an account for a doctor who has left, and checks that notification delivery is functioning correctly — all from a single admin panel.
