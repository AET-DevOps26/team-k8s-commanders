import { FormEvent, ReactNode, useEffect, useMemo, useState } from 'react'
import './App.css'
import {
  AuthSession,
  Appointment,
  AIQueryResponse,
  ClinicalNoteInput,
  UserProfile,
  VisitHistory,
  getAppointmentNote,
  getDoctorAppointments,
  getPatientAppointments,
  getPatientProfile,
  getPatientVisitHistory,
  listUsers,
  login,
  logout,
  queryAi,
  registerPatient,
  upsertAppointmentNote,
} from './clientApi'

const SESSION_KEY = 'caredesk.authSession'

const features = [
  {
    title: 'Appointment Booking',
    text: 'Patients find available slots, book visits, reschedule when plans change, and receive automatic reminders before each appointment.',
    icon: 'calendar',
  },
  {
    title: 'Patient Management',
    text: 'Doctors and clinic teams keep profiles, visit history, diagnoses, and structured clinical notes connected in one clear workspace.',
    icon: 'records',
  },
  {
    title: 'AI-Powered Assistance',
    text: 'A grounded clinical assistant helps doctors query patient history and guideline knowledge without searching through scattered records.',
    icon: 'spark',
  },
]

const workflowStats = [
  ['3 roles', 'Patient, doctor, admin'],
  ['24h', 'Reminder window'],
  ['1 view', 'Schedules, notes, history'],
]

type Route = '/' | '/login' | '/register' | '/patient' | '/doctor'

type AuthFormProps = {
  mode: 'login' | 'register'
  onAuthenticated: (session: AuthSession) => void
  onNavigate: (path: Route) => void
}

type PatientDashboardProps = {
  session: AuthSession
  onLogout: () => void
  onNavigate: (path: Route) => void
}

type PatientData = {
  profile: UserProfile
  appointments: Appointment[]
  visitHistory: VisitHistory
}

function getInitialRoute(): Route {
  const path = window.location.pathname

  if (
    path === '/login' ||
    path === '/register' ||
    path === '/patient' ||
    path === '/doctor'
  ) {
    return path
  }

  return '/'
}

function getStoredSession() {
  const rawSession = window.localStorage.getItem(SESSION_KEY)

  if (!rawSession) {
    return null
  }

  try {
    const session = JSON.parse(rawSession) as AuthSession

    if (session.accessToken && session.user?.id) {
      return session
    }
  } catch {
    window.localStorage.removeItem(SESSION_KEY)
  }

  return null
}

function saveSession(session: AuthSession) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

function clearSession() {
  window.localStorage.removeItem(SESSION_KEY)
}

function FeatureIcon({ type }: { type: string }) {
  if (type === 'calendar') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M7 3v4M17 3v4M4 9h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" />
        <path d="M8 13h3M13 13h3M8 16h3" />
      </svg>
    )
  }

  if (type === 'records') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M8 4h8l3 3v13H5V4h3Z" />
        <path d="M15 4v4h4M8 12h8M8 16h5M10.5 8h3" />
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 3l1.7 5.1L19 10l-5.3 1.9L12 17l-1.7-5.1L5 10l5.3-1.9L12 3Z" />
      <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15ZM5 14l.6 1.6L7 16l-1.4.4L5 18l-.6-1.6L3 16l1.4-.4L5 14Z" />
    </svg>
  )
}

function AppLink({
  to,
  className,
  children,
  onNavigate,
}: {
  to: Route
  className?: string
  children: ReactNode
  onNavigate: (path: Route) => void
}) {
  return (
    <a
      className={className}
      href={to}
      onClick={(event) => {
        event.preventDefault()
        onNavigate(to)
      }}
    >
      {children}
    </a>
  )
}

function ShellNav({
  session,
  onNavigate,
  onLogout,
}: {
  session: AuthSession | null
  onNavigate: (path: Route) => void
  onLogout?: () => void
}) {
  return (
    <nav className="site-nav" aria-label="Primary navigation">
      <AppLink className="brand" to="/" onNavigate={onNavigate}>
        <span className="brand-mark">+</span>
        <span>CareDesk</span>
      </AppLink>
      <div className="nav-links">
        <a href="/#features">Features</a>
        {session ? (
          <>
            {session.user.role === 'DOCTOR' ? (
              <AppLink to="/doctor" onNavigate={onNavigate}>
                Doctor dashboard
              </AppLink>
            ) : (
              <AppLink to="/patient" onNavigate={onNavigate}>
                Patient dashboard
              </AppLink>
            )}
            <button className="link-button" type="button" onClick={onLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <AppLink to="/login" onNavigate={onNavigate}>
              Login
            </AppLink>
            <AppLink className="nav-cta" to="/register" onNavigate={onNavigate}>
              Sign up
            </AppLink>
          </>
        )}
      </div>
    </nav>
  )
}

function LandingPage({
  session,
  onNavigate,
  onLogout,
}: {
  session: AuthSession | null
  onNavigate: (path: Route) => void
  onLogout: () => void
}) {
  return (
    <main className="landing-page">
      <ShellNav
        session={session}
        onNavigate={onNavigate}
        onLogout={onLogout}
      />

      <section className="hero-section">
        <div className="hero-copy">
          <p className="eyebrow">Unified clinic management</p>
          <h1>One clear desk for every outpatient workflow.</h1>
          <p className="hero-subheadline">
            CareDesk brings appointment booking, patient records, clinical notes,
            reminders, and AI-assisted history queries into one modern platform
            for patients, doctors, and clinic admins.
          </p>
          <div className="hero-actions">
            <AppLink
              className="primary-button"
              to={session ? '/patient' : '/register'}
              onNavigate={onNavigate}
            >
              {session ? 'Open dashboard' : 'Start with CareDesk'}
            </AppLink>
            <AppLink
              className="secondary-button"
              to={session ? '/patient' : '/login'}
              onNavigate={onNavigate}
            >
              {session ? 'Patient area' : 'Log in'}
            </AppLink>
          </div>
        </div>

        <div
          className="hero-visual"
          aria-label="Doctor consultation with CareDesk workflow preview"
        >
          <img
            src="https://images.unsplash.com/photo-1758691461935-202e2ef6b69f?auto=format&fit=crop&fm=jpg&q=80&w=1400"
            alt="Doctor speaking with a patient in a clinic office"
          />
          <div className="dashboard-card" aria-hidden="true">
            <div className="dashboard-header">
              <span>Today</span>
              <strong>8 appointments</strong>
            </div>
            <div className="schedule-row active">
              <span>09:30</span>
              <p>Maria Keller</p>
              <b>Booked</b>
            </div>
            <div className="schedule-row">
              <span>10:15</span>
              <p>Dr. Schmidt</p>
              <b>Notes</b>
            </div>
            <div className="ai-note">
              <span>AI</span>
              <p>Summarized last 5 visits with guideline references.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="proof-band" aria-label="CareDesk workflow summary">
        {workflowStats.map(([value, label]) => (
          <div className="proof-item" key={value}>
            <strong>{value}</strong>
            <span>{label}</span>
          </div>
        ))}
      </section>

      <section className="feature-section" id="features">
        <div className="section-heading">
          <p className="eyebrow">Core capabilities</p>
          <h2>Built around real clinic days, not disconnected tools.</h2>
        </div>

        <div className="feature-grid">
          {features.map((feature) => (
            <article className="feature-card" key={feature.title}>
              <div className="feature-icon">
                <FeatureIcon type={feature.icon} />
              </div>
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="split-section">
        <div>
          <p className="eyebrow">For patients, doctors, admins</p>
          <h2>Less admin friction. More useful context at care time.</h2>
          <p>
            Patients book without phone calls. Doctors see schedules, write
            structured notes, and query visit history in natural language.
            Admins manage users and clinic activity from one place.
          </p>
          <AppLink className="text-link" to="/register" onNavigate={onNavigate}>
            Create account
          </AppLink>
        </div>
        <img
          src="https://images.unsplash.com/photo-1584982751601-97dcc096659c?auto=format&fit=crop&fm=jpg&q=80&w=1200"
          alt="Healthcare team reviewing patient information on a tablet"
        />
      </section>
    </main>
  )
}

function AuthForm({ mode, onAuthenticated, onNavigate }: AuthFormProps) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const isRegister = mode === 'register'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const session = isRegister
        ? await registerPatient({ name, email, password })
        : await login({ email, password })

      onAuthenticated(session)
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : 'Authentication failed',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="landing-page auth-page">
      <ShellNav session={null} onNavigate={onNavigate} />
      <section className="auth-shell">
        <div className="auth-copy">
          <p className="eyebrow">Patient access</p>
          <h1>{isRegister ? 'Create patient account.' : 'Welcome back.'}</h1>
          <p>
            Welcome to CareDesk. Sign in and discover your health information at
            a glance.
          </p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <h2>{isRegister ? 'Sign up' : 'Login'}</h2>
          {isRegister && (
            <label>
              Full name
              <input
                autoComplete="name"
                onChange={(event) => setName(event.target.value)}
                required
                type="text"
                value={name}
              />
            </label>
          )}
          <label>
            Email
            <input
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </label>
          <label>
            Password
            <input
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              minLength={8}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </label>
          {error && <p className="form-error">{error}</p>}
          <button className="primary-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Please wait' : isRegister ? 'Create account' : 'Login'}
          </button>
          <button
            className="text-link button-reset"
            onClick={() => onNavigate(isRegister ? '/login' : '/register')}
            type="button"
          >
            {isRegister ? 'Already have an account?' : 'Need an account?'}
          </button>
        </form>
      </section>
    </main>
  )
}

function PatientDashboard({
  session,
  onLogout,
  onNavigate,
}: PatientDashboardProps) {
  const [patientData, setPatientData] = useState<PatientData | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const patientId = session.user.id

  const nextAppointment = useMemo(() => {
    if (!patientData) {
      return null
    }

    return patientData.appointments
      .filter((appointment) => appointment.status !== 'CANCELLED')
      .sort(
        (first, second) =>
          new Date(first.dateTime).getTime() -
          new Date(second.dateTime).getTime(),
      )[0]
  }, [patientData])

  useEffect(() => {
    let isActive = true

    async function loadPatientData() {
      setLoading(true)
      setError('')

      try {
        const [profile, appointmentsResponse, visitHistory] = await Promise.all([
          getPatientProfile(patientId, session.accessToken),
          getPatientAppointments(patientId, session.accessToken),
          getPatientVisitHistory(patientId, session.accessToken),
        ])

        if (isActive) {
          setPatientData({
            profile,
            appointments: appointmentsResponse.content,
            visitHistory,
          })
        }
      } catch (loadError) {
        if (isActive) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : 'Patient data could not be loaded',
          )
          setPatientData(null)
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadPatientData()

    return () => {
      isActive = false
    }
  }, [patientId, session.accessToken])

  if (session.user.role !== 'PATIENT') {
    return (
      <main className="landing-page app-page">
        <ShellNav
          session={session}
          onNavigate={onNavigate}
          onLogout={onLogout}
        />
        <section className="empty-state">
          <p className="eyebrow">Patient dashboard</p>
          <h1>Patient account required.</h1>
          <p>This dashboard only uses patient-scoped API endpoints.</p>
          <button className="primary-button" onClick={onLogout} type="button">
            Logout
          </button>
        </section>
      </main>
    )
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Patient dashboard</p>
            <h1>{patientData?.profile.name ?? session.user.name}</h1>
            <p>
              Appointment overview, visit history, and profile details loaded
              from CareDesk API.
            </p>
          </div>
          <button
            className="secondary-button"
            disabled={isLoading}
            onClick={() => window.location.reload()}
            type="button"
          >
            Refresh
          </button>
        </header>

        {isLoading && <StatusPanel title="Loading patient data" />}
        {error && <StatusPanel title="Patient API unavailable" text={error} />}

        {patientData && (
          <>
            <section className="patient-summary">
              <SummaryCard
                label="Upcoming"
                value={nextAppointment ? '1' : '0'}
                text={
                  nextAppointment
                    ? formatAppointmentDate(nextAppointment.dateTime)
                    : 'No scheduled appointment returned'
                }
              />
              <SummaryCard
                label="Appointments"
                value={String(patientData.appointments.length)}
                text="Loaded from patient appointments endpoint"
              />
              <SummaryCard
                label="Visit history"
                value={String(patientData.visitHistory.appointments.length)}
                text="Loaded from visit-history endpoint"
              />
            </section>

            <section className="dashboard-grid">
              <article className="dashboard-panel wide-panel">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Appointments</p>
                    <h2>My schedule</h2>
                  </div>
                </div>
                {patientData.appointments.length ? (
                  <div className="appointment-list">
                    {patientData.appointments.map((appointment) => (
                      <AppointmentRow
                        appointment={appointment}
                        key={appointment.id}
                      />
                    ))}
                  </div>
                ) : (
                  <EmptyPanel text="No appointments returned by API." />
                )}
              </article>

              <article className="dashboard-panel">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Profile</p>
                    <h2>Account</h2>
                  </div>
                </div>
                <dl className="profile-list">
                  <div>
                    <dt>Email</dt>
                    <dd>{patientData.profile.email}</dd>
                  </div>
                  <div>
                    <dt>Role</dt>
                    <dd>{patientData.profile.role}</dd>
                  </div>
                  <div>
                    <dt>Date of birth</dt>
                    <dd>{patientData.profile.dateOfBirth ?? 'Not provided'}</dd>
                  </div>
                  <div>
                    <dt>Phone</dt>
                    <dd>{patientData.profile.phoneNumber ?? 'Not provided'}</dd>
                  </div>
                </dl>
              </article>

              <article className="dashboard-panel wide-panel">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Visit history</p>
                    <h2>Clinical timeline</h2>
                  </div>
                </div>
                {patientData.visitHistory.notes?.length ? (
                  <div className="note-list">
                    {patientData.visitHistory.notes.map((note) => (
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
                ) : (
                  <EmptyPanel text="No clinical notes returned by API." />
                )}
              </article>
            </section>
          </>
        )}
      </section>
    </main>
  )
}

type DoctorDashboardProps = {
  session: AuthSession
  onLogout: () => void
  onNavigate: (path: Route) => void
}

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

function DoctorDashboard({
  session,
  onLogout,
  onNavigate,
}: DoctorDashboardProps) {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [users, setUsers] = useState<UserProfile[]>([])
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
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
          setError(
            loadError instanceof Error
              ? loadError.message
              : 'Doctor data could not be loaded',
          )
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
  }, [token])

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
            onClick={() => window.location.reload()}
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
  patientName,
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
          {patientName}
          {appointment.reason ? ` · ${appointment.reason}` : ''}
        </p>
      </div>
      <div className="appointment-row-actions">
        <span>{appointment.status}</span>
        <button
          className="link-button"
          onClick={onOpenPatient}
          type="button"
        >
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
          setError(
            loadError instanceof Error
              ? loadError.message
              : 'Patient record could not be loaded',
          )
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
          setError(
            loadError instanceof Error
              ? loadError.message
              : 'Existing note could not be loaded',
          )
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
      setError(
        submitError instanceof Error
          ? submitError.message
          : 'Clinical note could not be saved',
      )
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
      setError(
        draftError instanceof Error
          ? draftError.message
          : 'AI draft could not be generated',
      )
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
      setError(
        askError instanceof Error
          ? askError.message
          : 'AI assistant is unavailable',
      )
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

function SummaryCard({
  label,
  value,
  text,
}: {
  label: string
  value: string
  text: string
}) {
  return (
    <article className="summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{text}</p>
    </article>
  )
}

function AppointmentRow({ appointment }: { appointment: Appointment }) {
  return (
    <div className="appointment-item">
      <div>
        <strong>{formatAppointmentDate(appointment.dateTime)}</strong>
        <p>{appointment.reason ?? 'No reason provided'}</p>
      </div>
      <span>{appointment.status}</span>
    </div>
  )
}

function EmptyPanel({ text }: { text: string }) {
  return <p className="empty-panel">{text}</p>
}

function StatusPanel({ title, text }: { title: string; text?: string }) {
  return (
    <section className="status-panel">
      <strong>{title}</strong>
      {text && <p>{text}</p>}
    </section>
  )
}

function formatAppointmentDate(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export default function App() {
  const [route, setRoute] = useState<Route>(getInitialRoute)
  const [session, setSession] = useState<AuthSession | null>(getStoredSession)

  useEffect(() => {
    function handlePopState() {
      setRoute(getInitialRoute())
    }

    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [])

  function navigate(path: Route) {
    window.history.pushState({}, '', path)
    setRoute(path)
  }

  function handleAuthenticated(nextSession: AuthSession) {
    saveSession(nextSession)
    setSession(nextSession)
    navigate(nextSession.user.role === 'DOCTOR' ? '/doctor' : '/patient')
  }

  async function handleLogout() {
    if (session) {
      logout(session.accessToken).catch(() => {
        // Local sign-out must still work if backend session endpoint is unavailable.
      })
    }

    clearSession()
    setSession(null)
    navigate('/')
  }

  if (route === '/login' || route === '/register') {
    return (
      <AuthForm
        mode={route === '/register' ? 'register' : 'login'}
        onAuthenticated={handleAuthenticated}
        onNavigate={navigate}
      />
    )
  }

  if (route === '/patient') {
    if (!session) {
      return (
        <AuthForm
          mode="login"
          onAuthenticated={handleAuthenticated}
          onNavigate={navigate}
        />
      )
    }

    return (
      <PatientDashboard
        session={session}
        onLogout={handleLogout}
        onNavigate={navigate}
      />
    )
  }

  if (route === '/doctor') {
    if (!session) {
      return (
        <AuthForm
          mode="login"
          onAuthenticated={handleAuthenticated}
          onNavigate={navigate}
        />
      )
    }

    return (
      <DoctorDashboard
        session={session}
        onLogout={handleLogout}
        onNavigate={navigate}
      />
    )
  }

  return (
    <LandingPage
      session={session}
      onNavigate={navigate}
      onLogout={handleLogout}
    />
  )
}
