# Database schema

CareDesk uses database-per-service ownership. Each service has its own
PostgreSQL database in Docker Compose and Kubernetes. Cross-service relationships
are stored as UUID references, not database-level foreign keys, because related
records live in different databases.

## Schema lifecycle

- Java services use JPA/Hibernate entity mappings as the schema source.
- `application.yml` defaults to `spring.jpa.hibernate.ddl-auto=validate`.
- Local Docker Compose sets `SPRING_JPA_HIBERNATE_DDL_AUTO=update` so fresh dev
  databases are created automatically.
- Production Compose keeps `validate` unless an explicit `*_DDL_AUTO=update`
  override is provided.
- The AI assistant creates its SQLAlchemy tables on startup through
  `Base.metadata.create_all`.

## Database ownership overview

| Service | Database | Owned tables | Notes |
|---------|----------|--------------|-------|
| auth-service | `auth-db` | `users` | Source of truth for patients, doctors and admins |
| patient-service | `patient-db` | `appointments`, `doctor_slots` | Owns booking and availability data |
| notes-service | `notes-db` | `clinical_notes` | Owns doctor-authored visit notes |
| notification-service | `notification-db` | `notifications` | Owns notification records and email delivery state |
| ai-assistant | `ai-db` | `conversation_sessions`, `conversation_messages` | Owns persisted AI chat sessions and messages |

## auth-service

Source: `services/auth-service/src/main/java/com/caredesk/auth/model/User.java`

### `users`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Application-assigned primary key |
| `name` | string | no | Display name |
| `email` | string | no | Unique login email |
| `password` | string | no | Encoded password |
| `role` | enum string | no | `PATIENT`, `DOCTOR` or `ADMIN` |
| `enabled` | boolean | no | Soft-delete / account-enabled flag, defaults to `true` |
| `date_of_birth` | date | yes | Patient profile field |
| `phone_number` | string | yes | Patient profile field |
| `specialization` | string | yes | Doctor profile field |
| `license_number` | string | yes | Doctor profile field |
| `clinic_id` | UUID | yes | Clinic affiliation identifier |

## patient-service

Source:

- `services/patient-service/src/main/java/com/caredesk/patient/model/Appointment.java`
- `services/patient-service/src/main/java/com/caredesk/patient/model/DoctorSlot.java`

### `appointments`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Application-assigned primary key |
| `patient_id` | UUID | no | References auth-service `users.id` |
| `doctor_id` | UUID | no | References auth-service `users.id` |
| `date_time` | timestamp with offset | no | Appointment start time |
| `status` | enum string | no | OpenAPI `AppointmentStatus` |
| `duration` | integer | no | Duration in minutes, must be positive |
| `reason` | string | yes | Free-text booking reason |
| `patient_email` | string | yes | Captured at booking for notification delivery |

### `doctor_slots`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Generated primary key |
| `doctor_id` | UUID | no | References auth-service `users.id` |
| `start_at` | timestamp with offset | no | Slot start |
| `end_at` | timestamp with offset | no | Slot end; must be after `start_at` |
| `available` | boolean | no | Flips to `false` once booked |

Clinic endpoints currently act as an API scaffold. There is no patient-service
`clinics` table; clinic affiliation is represented by `users.clinic_id` in
auth-service.

## notes-service

Source:

- `services/notes-service/src/main/java/com/caredesk/notes/model/ClinicalNote.java`
- `services/notes-service/src/main/java/com/caredesk/notes/model/Diagnosis.java`

### `clinical_notes`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Generated primary key |
| `appointment_id` | UUID | no | References patient-service `appointments.id`; unique |
| `doctor_id` | UUID | no | References auth-service `users.id` |
| `content` | text | no | Free-text consultation findings and treatment summary |
| `diagnosis_code` | string | yes | Embedded diagnosis code, for example ICD-10 |
| `diagnosis_description` | string | yes | Embedded diagnosis description |
| `created_at` | timestamp with offset | no | Set on first persist when missing |

Constraint:

- `uk_clinical_notes_appointment` keeps one clinical note per appointment.

## notification-service

Source: `services/notification-service/src/main/java/com/caredesk/notification/model/Notification.java`

### `notifications`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Application-assigned primary key |
| `appointment_id` | UUID | yes | References patient-service `appointments.id` when appointment-related |
| `patient_id` | UUID | yes | References auth-service `users.id` when known |
| `message` | text | no | Delivered message body |
| `channel` | enum string | no | OpenAPI `NotificationChannel`, e.g. `EMAIL` |
| `type` | enum string | yes | Internal type: `CONFIRMATION`, `RESCHEDULE`, `CANCELLATION`, `REMINDER`, `GENERIC` |
| `recipient_email` | string | yes | Address used for email delivery |
| `delivered` | boolean | no | Whether SMTP accepted the email |
| `delivery_attempts` | integer | no | Number of delivery attempts |
| `sent_at` | timestamp with offset | no | Set on first persist when missing |

## ai-assistant

Source: `services/ai-assistant/db/orm.py`

### `conversation_sessions`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Generated primary key |
| `user_id` | UUID | no | Gateway-supplied owner id from auth-service |
| `patient_id` | UUID | yes | Patient context binding |
| `appointment_id` | UUID | yes | Appointment context binding |
| `title` | string(255) | yes | Chat title |
| `created_at` | timestamp with timezone | no | Creation timestamp |
| `updated_at` | timestamp with timezone | no | Updated on changes |

Index:

- `user_id`

### `conversation_messages`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | UUID | no | Generated primary key |
| `session_id` | UUID | no | Foreign key to `conversation_sessions.id`, cascade delete |
| `role` | string(16) | no | `user` or `assistant` |
| `content` | text | no | Message text |
| `sources` | JSON | yes | Grounding sources for assistant replies |
| `created_at` | timestamp with timezone | no | Creation timestamp |

Constraints and indexes:

- `session_id` foreign key cascades on session delete.
- `ck_conversation_messages_role` restricts `role` to `user` or `assistant`.
- `session_id` is indexed.
