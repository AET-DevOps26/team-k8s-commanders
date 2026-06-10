import { useEffect, useMemo, useState, type FormEvent } from 'react'
import type {
  AIQueryResponse,
  Appointment,
  ClinicalNoteInput,
  UserProfile,
  VisitHistory,
} from '../../clientApi'
import {
  getAppointmentNote,
  getDoctorAppointments,
  getPatientAppointments,
  getPatientProfile,
  getPatientVisitHistory,
  listUsers,
  queryAi,
  upsertAppointmentNote,
} from '../../clientApi'
import { formatAppointmentDate } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import type { DoctorDashboardProps } from '../../types/route'
import { ShellNav } from '../layout/ShellNav'
import { EmptyPanel } from '../ui/EmptyPanel'
import { StatusPanel } from '../ui/StatusPanel'
import { SummaryCard } from '../ui/SummaryCard'

function isUpcoming(appointment: Appointment) {
  return (
    appointment.status !== 'CANCELLED' &&
    appointment.status !== 'COMPLETED' &&
    new Date(appointment.dateTime).getTime() >= Date.now()
  )
}

function isToday(value: string) {
  const date = new Date(value)
  const now = new Date()
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  )
}

export function DoctorDashboard({
  session,
  onLogout,
  onNavigate,
}: DoctorDashboardProps) {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [users, setUsers] = useState<UserProfile[]>([])
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(null)
  const doctorId = session.user.id
  const token = session.accessToken

  useEffect(() => {
    let isActive = true

    async function loadDoctorData() {
      setLoading(true)
      setError('')

      try {
        const [appointmentsResponse, usersResponse] = await Promise.all([
          getDoctorAppointments(token),
          listUsers(token),
        ])

        if (isActive) {
          setAppointments(appointmentsResponse.content)
          setUsers(usersResponse.content)
        }
      } catch (loadError) {
        if (isActive) {
          setError(userMessage('Doctor data could not be loaded. Please try again in a moment.'))
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadDoctorData()

    return () => {
      isActive = false
    }
  }, [token, reloadKey])

  const userMap = useMemo(() => {
    const map = new Map<string, UserProfile>()
    users.forEach((user) => map.set(user.id, user))
    return map
  }, [users])

  const patients = useMemo(
    () => users.filter((user) => user.role === 'PATIENT'),
    [users],
  )

  const doctorAppointments = useMemo(
    () =>
      appointments
        .filter((appointment) => appointment.doctorId === doctorId)
        .sort(
          (first, second) =>
            new Date(first.dateTime).getTime() -
            new Date(second.dateTime).getTime(),
        ),
    [appointments, doctorId],
  )

  const upcomingAppointments = useMemo(
    () => doctorAppointments.filter(isUpcoming),
    [doctorAppointments],
  )

  const todaysCount = useMemo(
    () => upcomingAppointments.filter((appt) => isToday(appt.dateTime)).length,
    [upcomingAppointments],
  )

  const distinctPatientCount = useMemo(
    () => new Set(doctorAppointments.map((appt) => appt.patientId)).size,
    [doctorAppointments],
  )

  if (session.user.role !== 'DOCTOR') {
    return (
      <main className="landing-page app-page">
        <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
        <section className="empty-state">
          <p className="eyebrow">Doctor dashboard</p>
          <h1>Doctor account required.</h1>
          <p>This dashboard is only available to clinicians.</p>
          <button className="primary-button" onClick={onLogout} type="button">
            Logout
          </button>
        </section>
      </main>
    )
  }

  function patientName(patientId: string) {
    return userMap.get(patientId)?.name ?? patientId
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Doctor dashboard</p>
            <h1>{session.user.name}</h1>
            <p>
              Upcoming appointments and patient records, loaded from the CareDesk
              API.
            </p>
          </div>
          <button
            className="secondary-button"
            disabled={isLoading}
            onClick={() => setReloadKey((current) => current + 1)}
            type="button"
          >
            Refresh
          </button>
        </header>

        {isLoading && <StatusPanel title="Loading doctor data" />}
        {error && <StatusPanel title="Doctor API unavailable" text={error} />}

        {!isLoading && !error && (
          <>
            <section className="patient-summary">
              <SummaryCard
                label="Upcoming"
                value={String(upcomingAppointments.length)}
                text="Scheduled appointments ahead"
              />
              <SummaryCard
                label="Today"
                value={String(todaysCount)}
                text="Appointments today"
              />
              <SummaryCard
                label="Patients"
                value={String(distinctPatientCount)}
                text="Distinct patients on your schedule"
              />
            </section>

            <section className="dashboard-grid">
              <article className="dashboard-panel wide-panel">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Appointments</p>
                    <h2>Upcoming schedule</h2>
                  </div>
                </div>
                {upcomingAppointments.length ? (
                  <div className="appointment-list">
                    {upcomingAppointments.map((appointment) => (
                      <DoctorAppointmentRow
                        appointment={appointment}
                        key={appointment.id}
                        patientName={patientName(appointment.patientId)}
                        onOpenPatient={() =>
                          setSelectedPatientId(appointment.patientId)
                        }
                      />
                    ))}
                  </div>
                ) : (
                  <EmptyPanel text="No upcoming appointments booked with you." />
                )}
              </article>

              <article className="dashboard-panel wide-panel">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Patientenakte</p>
                    <h2>Patient records</h2>
                  </div>
                </div>
                <PatientSearch
                  patients={patients}
                  selectedPatientId={selectedPatientId}
                  onSelect={setSelectedPatientId}
                />
                {selectedPatientId ? (
                  <PatientRecord
                    key={selectedPatientId}
                    patientId={selectedPatientId}
                    profile={userMap.get(selectedPatientId) ?? null}
                    token={token}
                  />
                ) : (
                  <EmptyPanel text="Search and select a patient to open their record." />
                )}
              </article>
            </section>
          </>
        )}
      </section>
    </main>
  )
}

function DoctorAppointmentRow({
  appointment,
  patientName: name,
  onOpenPatient,
}: {
  appointment: Appointment
  patientName: string
  onOpenPatient: () => void
}) {
  return (
    <div className="appointment-item">
      <div>
        <strong>{formatAppointmentDate(appointment.dateTime)}</strong>
        <p>
          {name}
          {appointment.reason ? ` · ${appointment.reason}` : ''}
        </p>
      </div>
      <div className="appointment-row-actions">
        <span>{appointment.status}</span>
        <button className="link-button" onClick={onOpenPatient} type="button">
          Open record
        </button>
      </div>
    </div>
  )
}

function PatientSearch({
  patients,
  selectedPatientId,
  onSelect,
}: {
  patients: UserProfile[]
  selectedPatientId: string | null
  onSelect: (patientId: string) => void
}) {
  const [query, setQuery] = useState('')

  const matches = useMemo(() => {
    const trimmed = query.trim().toLowerCase()
    if (!trimmed) {
      return patients
    }
    return patients.filter(
      (patient) =>
        patient.name.toLowerCase().includes(trimmed) ||
        patient.email.toLowerCase().includes(trimmed),
    )
  }, [patients, query])

  return (
    <div className="patient-search">
      <input
        className="search-input"
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search patients by name or email"
        type="search"
        value={query}
      />
      {matches.length ? (
        <ul className="patient-results">
          {matches.slice(0, 8).map((patient) => (
            <li key={patient.id}>
              <button
                className={
                  patient.id === selectedPatientId
                    ? 'patient-result is-active'
                    : 'patient-result'
                }
                onClick={() => onSelect(patient.id)}
                type="button"
              >
                <strong>{patient.name}</strong>
                <span>{patient.email}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="empty-panel">No patients match your search.</p>
      )}
    </div>
  )
}

type PatientRecordData = {
  profile: UserProfile
  appointments: Appointment[]
  visitHistory: VisitHistory
}

function PatientRecord({
  patientId,
  profile: directoryProfile,
  token,
}: {
  patientId: string
  profile: UserProfile | null
  token: string
}) {
  const [data, setData] = useState<PatientRecordData | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [selectedAppointmentId, setSelectedAppointmentId] = useState<
    string | null
  >(null)

  useEffect(() => {
    let isActive = true

    async function loadRecord() {
      setLoading(true)
      setError('')

      try {
        const [profile, appointmentsResponse, visitHistory] = await Promise.all([
          getPatientProfile(patientId, token),
          getPatientAppointments(patientId, token),
          getPatientVisitHistory(patientId, token),
        ])

        if (isActive) {
          setData({
            profile,
            appointments: appointmentsResponse.content,
            visitHistory,
          })
        }
      } catch (loadError) {
        if (isActive) {
          setError(userMessage('Patient record could not be loaded. Please try again in a moment.'))
          setData(null)
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadRecord()

    return () => {
      isActive = false
    }
  }, [patientId, token])

  const sortedAppointments = useMemo(() => {
    if (!data) {
      return []
    }
    return [...data.appointments].sort(
      (first, second) =>
        new Date(second.dateTime).getTime() -
        new Date(first.dateTime).getTime(),
    )
  }, [data])

  const displayName = directoryProfile?.name ?? patientId
  const displayEmail = directoryProfile?.email

  return (
    <div className="patient-record">
      <div className="patient-record-head">
        <div>
          <h3>{displayName}</h3>
          {displayEmail && <p>{displayEmail}</p>}
        </div>
      </div>

      {isLoading && <StatusPanel title="Loading patient record" />}
      {error && <StatusPanel title="Patient record unavailable" text={error} />}

      {data && (
        <>
          <dl className="profile-list">
            <div>
              <dt>Date of birth</dt>
              <dd>{data.profile.dateOfBirth ?? 'Not provided'}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{data.profile.phoneNumber ?? 'Not provided'}</dd>
            </div>
          </dl>

          <div className="record-section">
            <p className="eyebrow">Recent appointments</p>
            {sortedAppointments.length ? (
              <div className="appointment-list">
                {sortedAppointments.map((appointment) => (
                  <button
                    className={
                      appointment.id === selectedAppointmentId
                        ? 'appointment-item appointment-select is-active'
                        : 'appointment-item appointment-select'
                    }
                    key={appointment.id}
                    onClick={() => setSelectedAppointmentId(appointment.id)}
                    type="button"
                  >
                    <div>
                      <strong>
                        {formatAppointmentDate(appointment.dateTime)}
                      </strong>
                      <p>{appointment.reason ?? 'No reason provided'}</p>
                    </div>
                    <span>{appointment.status}</span>
                  </button>
                ))}
              </div>
            ) : (
              <EmptyPanel text="No appointments for this patient." />
            )}
          </div>

          {data.visitHistory.notes?.length ? (
            <div className="record-section">
              <p className="eyebrow">Visit history</p>
              <div className="note-list">
                {data.visitHistory.notes.map((note) => (
                  <div className="note-item" key={note.id}>
                    <strong>{formatAppointmentDate(note.createdAt)}</strong>
                    <p>{note.content}</p>
                    {note.diagnosis && (
                      <span>
                        {note.diagnosis.code} · {note.diagnosis.description}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {selectedAppointmentId ? (
            <NoteEditor
              appointmentId={selectedAppointmentId}
              patientId={patientId}
              token={token}
            />
          ) : (
            <EmptyPanel text="Select an appointment to write a clinical note." />
          )}

          <AiAssistant
            patientId={patientId}
            appointmentId={selectedAppointmentId}
            token={token}
          />
        </>
      )}
    </div>
  )
}

function NoteEditor({
  appointmentId,
  patientId,
  token,
}: {
  appointmentId: string
  patientId: string
  token: string
}) {
  const [content, setContent] = useState('')
  const [diagnosisCode, setDiagnosisCode] = useState('')
  const [diagnosisDescription, setDiagnosisDescription] = useState('')
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const [isDrafting, setDrafting] = useState(false)

  useEffect(() => {
    let isActive = true

    async function loadNote() {
      setStatus('')
      setError('')

      try {
        const note = await getAppointmentNote(appointmentId, token)
        if (isActive && note) {
          setContent(note.content)
          setDiagnosisCode(note.diagnosis?.code ?? '')
          setDiagnosisDescription(note.diagnosis?.description ?? '')
        } else if (isActive) {
          setContent('')
          setDiagnosisCode('')
          setDiagnosisDescription('')
        }
      } catch (loadError) {
        if (isActive) {
          setError(userMessage('Existing note could not be loaded. Please try again in a moment.'))
        }
      }
    }

    loadNote()

    return () => {
      isActive = false
    }
  }, [appointmentId, token])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setStatus('')
    setSubmitting(true)

    const input: ClinicalNoteInput = { content }
    if (diagnosisCode.trim() && diagnosisDescription.trim()) {
      input.diagnosis = {
        code: diagnosisCode.trim(),
        description: diagnosisDescription.trim(),
      }
    }

    try {
      await upsertAppointmentNote(appointmentId, input, token)
      setStatus('Clinical note saved.')
    } catch (submitError) {
      setError(userMessage('Clinical note could not be saved. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDraft() {
    setError('')
    setStatus('')
    setDrafting(true)

    try {
      const response = await queryAi(
        {
          patientId,
          appointmentId,
          query:
            'Draft a concise clinical note for this appointment, summarizing the relevant patient history.',
        },
        token,
      )
      setContent((current) =>
        current ? `${current}\n\n${response.answer}` : response.answer,
      )
      setStatus('AI draft inserted — review before saving.')
    } catch (draftError) {
      setError(userMessage('AI draft could not be generated. Please try again.'))
    } finally {
      setDrafting(false)
    }
  }

  return (
    <form className="note-form" onSubmit={handleSubmit}>
      <div className="note-form-head">
        <p className="eyebrow">Clinical note</p>
        <button
          className="link-button"
          disabled={isDrafting}
          onClick={handleDraft}
          type="button"
        >
          {isDrafting ? 'Drafting…' : 'Draft with AI'}
        </button>
      </div>
      <label>
        Note
        <textarea
          onChange={(event) => setContent(event.target.value)}
          required
          rows={5}
          value={content}
        />
      </label>
      <div className="note-form-row">
        <label>
          Diagnosis code
          <input
            onChange={(event) => setDiagnosisCode(event.target.value)}
            placeholder="e.g. J06.9"
            type="text"
            value={diagnosisCode}
          />
        </label>
        <label>
          Diagnosis description
          <input
            onChange={(event) => setDiagnosisDescription(event.target.value)}
            placeholder="e.g. Acute upper respiratory infection"
            type="text"
            value={diagnosisDescription}
          />
        </label>
      </div>
      {error && <p className="form-error">{error}</p>}
      {status && <p className="form-status">{status}</p>}
      <button className="primary-button" disabled={isSubmitting} type="submit">
        {isSubmitting ? 'Saving…' : 'Save note'}
      </button>
    </form>
  )
}

function AiAssistant({
  patientId,
  appointmentId,
  token,
}: {
  patientId: string
  appointmentId: string | null
  token: string
}) {
  const [query, setQuery] = useState('')
  const [answer, setAnswer] = useState<AIQueryResponse | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(false)

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await queryAi(
        {
          patientId,
          ...(appointmentId ? { appointmentId } : {}),
          query,
        },
        token,
      )
      setAnswer(response)
    } catch (askError) {
      setError(userMessage('AI assistant is unavailable. Please try again in a moment.'))
      setAnswer(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="ai-assistant" onSubmit={handleAsk}>
      <p className="eyebrow">AI assistant</p>
      <label>
        Ask about this patient&apos;s history
        <input
          onChange={(event) => setQuery(event.target.value)}
          placeholder="e.g. Summarize the last three visits"
          required
          type="text"
          value={query}
        />
      </label>
      {error && <p className="form-error">{error}</p>}
      <button className="secondary-button" disabled={isLoading} type="submit">
        {isLoading ? 'Asking…' : 'Ask AI'}
      </button>
      {answer && (
        <div className="ai-answer">
          <p>{answer.answer}</p>
          {answer.sources?.length ? (
            <ul className="ai-sources">
              {answer.sources.map((source, index) => (
                <li key={`${source}-${index}`}>{source}</li>
              ))}
            </ul>
          ) : null}
        </div>
      )}
    </form>
  )
}
