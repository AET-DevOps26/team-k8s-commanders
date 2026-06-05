import { FormEvent, ReactNode, useEffect, useMemo, useState } from 'react'
import './App.css'
import {
  AuthSession,
  Appointment,
  ScheduleSlot,
  UserProfile,
  VisitHistory,
  bookAppointment,
  cancelAppointment,
  changeUserPassword,
  getDoctorSchedule,
  getPatientAppointments,
  getPatientVisitHistory,
  getUserProfile,
  listDoctors,
  login,
  logout,
  registerPatient,
  rescheduleAppointment,
  updateUserProfile,
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

type Route = '/' | '/login' | '/register' | '/patient' | '/patient/profile' | '/patient/book'

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

type PatientProfileProps = PatientDashboardProps & {
  onSessionUpdated: (session: AuthSession) => void
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
    path === '/patient/profile' ||
    path === '/patient/book'
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

function userMessage(fallback: string) {
  return fallback
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
            <AppLink to="/patient" onNavigate={onNavigate}>
              Patient dashboard
            </AppLink>
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

function PatientSubNav({
  active,
  onNavigate,
}: {
  active: 'dashboard' | 'profile' | 'book'
  onNavigate: (path: Route) => void
}) {
  const items: Array<{ key: typeof active; label: string; route: Route }> = [
    { key: 'dashboard', label: 'Dashboard', route: '/patient' },
    { key: 'profile', label: 'Profile', route: '/patient/profile' },
    { key: 'book', label: 'Book appointment', route: '/patient/book' },
  ]

  return (
    <div className="patient-tabs" role="navigation" aria-label="Patient navigation">
      {items.map((item) => (
        <button
          className={active === item.key ? 'active' : ''}
          key={item.key}
          onClick={() => onNavigate(item.route)}
          type="button"
        >
          {item.label}
        </button>
      ))}
    </div>
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
  const [phoneNumber, setPhoneNumber] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const isRegister = mode === 'register'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const session = isRegister
        ? await registerPatient({ name, email, password, phoneNumber, dateOfBirth })
        : await login({ email, password })

      onAuthenticated(session)
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? userMessage(isRegister ? 'Account could not be created. Please check your details and try again.' : 'Login failed. Please check your email and password.')
          : userMessage('Something went wrong. Please try again.'),
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
            <>
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
              <label>
                Phone number
                <input
                  autoComplete="tel"
                  onChange={(event) => setPhoneNumber(event.target.value)}
                  required
                  type="tel"
                  value={phoneNumber}
                />
              </label>
              <label>
                Date of birth
                <input
                  onChange={(event) => setDateOfBirth(event.target.value)}
                  required
                  type="date"
                  value={dateOfBirth}
                />
              </label>
            </>
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
  const [reloadKey, setReloadKey] = useState(0)
  const patientId = session.user.id

  const upcomingAppointments = useMemo(() => {
    if (!patientData) {
      return []
    }

    const now = Date.now()

    return patientData.appointments
      .filter((appointment) => appointment.status !== 'CANCELLED')
      .filter((appointment) => new Date(appointment.dateTime).getTime() >= now)
      .sort(
        (first, second) =>
          new Date(first.dateTime).getTime() -
          new Date(second.dateTime).getTime(),
      )
  }, [patientData])

  const pastVisits = useMemo(() => {
    if (!patientData) {
      return []
    }

    const now = Date.now()

    return patientData.visitHistory.appointments.filter(
      (appointment) =>
        appointment.status === 'COMPLETED' ||
        new Date(appointment.dateTime).getTime() < now,
    )
  }, [patientData])

  const nextAppointment = useMemo(() => {
    return upcomingAppointments[0] ?? null
  }, [upcomingAppointments])

  useEffect(() => {
    let isActive = true

    async function loadPatientData() {
      setLoading(true)
      setError('')

      try {
        const [profile, appointmentsResponse, visitHistory] = await Promise.all([
          getUserProfile(patientId, session.accessToken),
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
              ? userMessage('Your dashboard could not be loaded. Please try again in a moment.')
              : userMessage('Your dashboard could not be loaded. Please try again in a moment.'),
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
  }, [patientId, session.accessToken, reloadKey])

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
          <p>Please sign in with a patient account to view this area.</p>
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
        <PatientSubNav active="dashboard" onNavigate={onNavigate} />
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Patient dashboard</p>
            <h1>{patientData?.profile.name ?? session.user.name}</h1>
            <p>
              View upcoming appointments, recent visits, and your account details.
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

        {isLoading && <StatusPanel title="Loading your dashboard" />}
        {error && <StatusPanel title="We could not load your dashboard" text={error} />}

        {patientData && (
          <>
            <section className="patient-summary">
              <SummaryCard
                label="Upcoming"
                value={String(upcomingAppointments.length)}
                text={
                  nextAppointment
                    ? formatAppointmentDate(nextAppointment.dateTime)
                    : 'No upcoming appointment'
                }
              />
              <SummaryCard
                label="Appointments"
                value={String(patientData.appointments.length)}
                text="Your booked visits"
              />
              <SummaryCard
                label="Visit history"
                value={String(pastVisits.length)}
                text="Past care activity"
              />
            </section>

            <section className="quick-actions" aria-label="Patient actions">
              <button
                className="secondary-button"
                onClick={() => onNavigate('/patient/profile')}
                type="button"
              >
                Edit profile
              </button>
              <button
                className="primary-button"
                onClick={() => onNavigate('/patient/book')}
                type="button"
              >
                Book appointment
              </button>
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
                        onChanged={() => setReloadKey((current) => current + 1)}
                        token={session.accessToken}
                      />
                    ))}
                  </div>
                ) : (
                  <EmptyPanel text="No appointments booked yet." />
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
                  <EmptyPanel text="No visit notes available yet." />
                )}
              </article>
            </section>
          </>
        )}
      </section>
    </main>
  )
}

function PatientProfilePage({
  session,
  onLogout,
  onNavigate,
  onSessionUpdated,
}: PatientProfileProps) {
  const [profile, setProfile] = useState<UserProfile>(session.user)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [profileStatus, setProfileStatus] = useState('')
  const [passwordStatus, setPasswordStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const userId = session.user.id

  useEffect(() => {
    let isActive = true

    async function loadProfile() {
      setLoading(true)
      setError('')

      try {
        const loadedProfile = await getUserProfile(userId, session.accessToken)

        if (isActive) {
          setProfile(loadedProfile)
        }
      } catch {
        if (isActive) {
          setError(userMessage('Your profile could not be loaded. Please try again in a moment.'))
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      isActive = false
    }
  }, [session.accessToken, userId])

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setProfileStatus('')

    try {
      const updatedProfile = await updateUserProfile(userId, session.accessToken, profile)
      const updatedSession = { ...session, user: updatedProfile }
      saveSession(updatedSession)
      onSessionUpdated(updatedSession)
      setProfile(updatedProfile)
      setProfileStatus('Profile updated')
    } catch {
      setError(userMessage('Your changes could not be saved. Please check your details and try again.'))
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setPasswordStatus('')

    try {
      await changeUserPassword(userId, session.accessToken, {
        currentPassword,
        newPassword,
      })
      setCurrentPassword('')
      setNewPassword('')
      setPasswordStatus('Password changed')
    } catch {
      setError(userMessage('Your password could not be changed. Please check your current password.'))
    }
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <PatientSubNav active="profile" onNavigate={onNavigate} />
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Profile</p>
            <h1>Account settings</h1>
            <p>Manage personal data, email, and password for your CareDesk account.</p>
          </div>
        </header>

        {isLoading && <StatusPanel title="Loading your profile" />}
        {error && <StatusPanel title="We could not update your account" text={error} />}

        <section className="settings-grid">
          <form className="auth-card settings-card" onSubmit={handleProfileSubmit}>
            <h2>Personal data</h2>
            <label>
              Full name
              <input
                autoComplete="name"
                onChange={(event) => setProfile({ ...profile, name: event.target.value })}
                required
                type="text"
                value={profile.name}
              />
            </label>
            <label>
              Email
              <input
                autoComplete="email"
                onChange={(event) => setProfile({ ...profile, email: event.target.value })}
                required
                type="email"
                value={profile.email}
              />
            </label>
            <label>
              Phone number
              <input
                autoComplete="tel"
                onChange={(event) => setProfile({ ...profile, phoneNumber: event.target.value })}
                type="tel"
                value={profile.phoneNumber ?? ''}
              />
            </label>
            <label>
              Date of birth
              <input
                onChange={(event) => setProfile({ ...profile, dateOfBirth: event.target.value })}
                type="date"
                value={profile.dateOfBirth ?? ''}
              />
            </label>
            <label>
              Role
              <input readOnly type="text" value={profile.role} />
            </label>
            {profileStatus && <p className="form-success">{profileStatus}</p>}
            <button className="primary-button" type="submit">
              Save profile
            </button>
          </form>

          <form className="auth-card settings-card" onSubmit={handlePasswordSubmit}>
            <h2>Password</h2>
            <label>
              Current password
              <input
                autoComplete="current-password"
                minLength={8}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
                type="password"
                value={currentPassword}
              />
            </label>
            <label>
              New password
              <input
                autoComplete="new-password"
                minLength={8}
                onChange={(event) => setNewPassword(event.target.value)}
                required
                type="password"
                value={newPassword}
              />
            </label>
            {passwordStatus && <p className="form-success">{passwordStatus}</p>}
            <button className="primary-button" type="submit">
              Change password
            </button>
          </form>
        </section>
      </section>
    </main>
  )
}

function PatientBookingPage({
  session,
  onLogout,
  onNavigate,
}: PatientDashboardProps) {
  const [query, setQuery] = useState('')
  const [specialization, setSpecialization] = useState('')
  const [doctors, setDoctors] = useState<UserProfile[]>([])
  const [selectedDoctor, setSelectedDoctor] = useState<UserProfile | null>(null)
  const [slots, setSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [reason, setReason] = useState('')
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(false)

  async function searchDoctors(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    setLoading(true)
    setError('')
    setStatus('')

    try {
      const response = await listDoctors(session.accessToken, {
        q: query,
        specialization,
        size: 12,
      })
      setDoctors(response.content)
      if (!response.content.some((doctor) => doctor.id === selectedDoctor?.id)) {
        setSelectedDoctor(null)
        setSlots([])
        setSelectedSlot(null)
      }
    } catch {
      setError(userMessage('Doctors could not be loaded. Please try again in a moment.'))
    } finally {
      setLoading(false)
    }
  }

  async function selectDoctor(doctor: UserProfile) {
    setSelectedDoctor(doctor)
    setSelectedSlot(null)
    setError('')
    setStatus('')

    try {
      const schedule = await getDoctorSchedule(doctor.id, session.accessToken)
      setSlots(schedule.slots)
    } catch {
      setSlots([])
      setError(userMessage('Available times could not be loaded. Please choose another doctor or try again.'))
    }
  }

  async function handleBooking() {
    if (!selectedDoctor || !selectedSlot) {
      setError('Please select a doctor and time slot')
      return
    }

    setError('')
    setStatus('')

    try {
      const duration = Math.round(
        (new Date(selectedSlot.endAt).getTime() - new Date(selectedSlot.startAt).getTime()) / 60000,
      )
      await bookAppointment(session.accessToken, {
        patientId: session.user.id,
        doctorId: selectedDoctor.id,
        dateTime: selectedSlot.startAt,
        duration,
        reason: reason || undefined,
      })
      setStatus('Appointment booked')
      setReason('')
      await selectDoctor(selectedDoctor)
    } catch {
      setError(userMessage('This appointment could not be booked. Please choose another time or try again.'))
    }
  }

  useEffect(() => {
    searchDoctors()
    // Initial doctor list should load once for the active session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session.accessToken])

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <PatientSubNav active="book" onNavigate={onNavigate} />
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Booking</p>
            <h1>Find a doctor</h1>
            <p>Search CareDesk doctors, choose an available slot, and book directly.</p>
          </div>
        </header>

        {error && <StatusPanel title="We could not complete your booking" text={error} />}
        {status && <StatusPanel title={status} text="Your appointment is now listed in your dashboard." />}

        <section className="booking-grid">
          <form className="auth-card booking-search" onSubmit={searchDoctors}>
            <h2>Doctor search</h2>
            <label>
              Search
              <input
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Name or specialization"
                type="search"
                value={query}
              />
            </label>
            <label>
              Specialization
              <input
                onChange={(event) => setSpecialization(event.target.value)}
                placeholder="General Medicine"
                type="text"
                value={specialization}
              />
            </label>
            <button className="primary-button" disabled={isLoading} type="submit">
              {isLoading ? 'Searching' : 'Search doctors'}
            </button>
          </form>

          <div className="doctor-results">
            {doctors.length ? (
              doctors.map((doctor) => (
                <button
                  className={selectedDoctor?.id === doctor.id ? 'doctor-card active' : 'doctor-card'}
                  key={doctor.id}
                  onClick={() => selectDoctor(doctor)}
                  type="button"
                >
                  <strong>{doctor.name}</strong>
                  <span>{doctor.specialization ?? 'CareDesk doctor'}</span>
                  <small>{doctor.email}</small>
                </button>
              ))
            ) : (
              <EmptyPanel text="No doctors found. Try another search." />
            )}
          </div>
        </section>

        {selectedDoctor && (
          <section className="calendar-panel dashboard-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Calendar</p>
                <h2>{selectedDoctor.name}</h2>
              </div>
            </div>
            <div className="slot-grid">
              {slots.length ? (
                slots.map((slot) => (
                  <button
                    className={selectedSlot?.startAt === slot.startAt ? 'slot-button active' : 'slot-button'}
                    key={`${slot.startAt}-${slot.endAt}`}
                    onClick={() => setSelectedSlot(slot)}
                    type="button"
                  >
                    <span>{formatAppointmentDate(slot.startAt)}</span>
                    <strong>{formatTimeRange(slot)}</strong>
                  </button>
                ))
              ) : (
                <EmptyPanel text="No available times right now." />
              )}
            </div>
            <label className="reason-field">
              Reason
              <textarea
                onChange={(event) => setReason(event.target.value)}
                placeholder="Short reason for the visit"
                rows={3}
                value={reason}
              />
            </label>
            <div className="quick-actions">
              <button
                className="secondary-button"
                onClick={() => onNavigate('/patient')}
                type="button"
              >
                Back to dashboard
              </button>
              <button className="primary-button" onClick={handleBooking} type="button">
                Book selected slot
              </button>
            </div>
          </section>
        )}
      </section>
    </main>
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

function AppointmentRow({
  appointment,
  onChanged,
  token,
}: {
  appointment: Appointment
  onChanged: () => void
  token: string
}) {
  const [availableSlots, setAvailableSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [isMoving, setMoving] = useState(false)
  const [message, setMessage] = useState('')
  const [isBusy, setBusy] = useState(false)
  const canChange = appointment.status !== 'CANCELLED' && appointment.status !== 'COMPLETED'

  async function handleCancel() {
    if (!canChange) {
      return
    }

    setBusy(true)
    setMessage('')

    try {
      await cancelAppointment(token, appointment.id)
      setMessage('Appointment cancelled')
      onChanged()
    } catch {
      setMessage('Appointment could not be cancelled. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  async function openMoveOptions() {
    setMoving((current) => !current)
    setMessage('')

    if (availableSlots.length) {
      return
    }

    setBusy(true)

    try {
      const schedule = await getDoctorSchedule(appointment.doctorId, token)
      setAvailableSlots(schedule.slots)
    } catch {
      setMessage('Available times could not be loaded. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  async function handleReschedule() {
    if (!selectedSlot) {
      setMessage('Please choose a new time.')
      return
    }

    setBusy(true)
    setMessage('')

    try {
      await rescheduleAppointment(token, appointment.id, {
        dateTime: selectedSlot.startAt,
        duration: slotDuration(selectedSlot),
      })
      setMessage('Appointment moved')
      setMoving(false)
      setSelectedSlot(null)
      onChanged()
    } catch {
      setMessage('Appointment could not be moved. Please choose another time.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="appointment-item">
      <div>
        <strong>{formatAppointmentDate(appointment.dateTime)}</strong>
        <p>{appointment.reason ?? 'No reason provided'}</p>
        {message && <small>{message}</small>}
      </div>
      <div className="appointment-actions">
        <span>{appointment.status}</span>
        {canChange && (
          <>
            <button disabled={isBusy} onClick={openMoveOptions} type="button">
              Move
            </button>
            <button disabled={isBusy} onClick={handleCancel} type="button">
              Cancel
            </button>
          </>
        )}
      </div>
      {isMoving && (
        <div className="move-panel">
          {availableSlots.length ? (
            <>
              <div className="move-slot-grid">
                {availableSlots.map((slot) => (
                  <button
                    className={selectedSlot?.startAt === slot.startAt ? 'active' : ''}
                    key={`${appointment.id}-${slot.startAt}`}
                    onClick={() => setSelectedSlot(slot)}
                    type="button"
                  >
                    <strong>{formatAppointmentDate(slot.startAt)}</strong>
                    <span>{formatTimeRange(slot)}</span>
                  </button>
                ))}
              </div>
              <button
                className="primary-button"
                disabled={isBusy}
                onClick={handleReschedule}
                type="button"
              >
                Confirm new time
              </button>
            </>
          ) : (
            <EmptyPanel text="No other times available right now." />
          )}
        </div>
      )}
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

function formatTimeRange(slot: ScheduleSlot) {
  const formatter = new Intl.DateTimeFormat('de-DE', {
    hour: '2-digit',
    minute: '2-digit',
  })

  return `${formatter.format(new Date(slot.startAt))} - ${formatter.format(new Date(slot.endAt))}`
}

function slotDuration(slot: ScheduleSlot) {
  return Math.round(
    (new Date(slot.endAt).getTime() - new Date(slot.startAt).getTime()) / 60000,
  )
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
    navigate('/patient')
  }

  function handleSessionUpdated(nextSession: AuthSession) {
    setSession(nextSession)
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

  if (route === '/patient' || route === '/patient/profile' || route === '/patient/book') {
    if (!session) {
      return (
        <AuthForm
          mode="login"
          onAuthenticated={handleAuthenticated}
          onNavigate={navigate}
        />
      )
    }

    if (route === '/patient/profile') {
      return (
        <PatientProfilePage
          session={session}
          onLogout={handleLogout}
          onNavigate={navigate}
          onSessionUpdated={handleSessionUpdated}
        />
      )
    }

    if (route === '/patient/book') {
      return (
        <PatientBookingPage
          session={session}
          onLogout={handleLogout}
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

  return (
    <LandingPage
      session={session}
      onNavigate={navigate}
      onLogout={handleLogout}
    />
  )
}
