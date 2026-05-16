import './App.css'

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

export default function App() {
  return (
    <main className="landing-page">
      <nav className="site-nav" aria-label="Primary navigation">
        <a className="brand" href="/" aria-label="CareDesk home">
          <span className="brand-mark">+</span>
          <span>CareDesk</span>
        </a>
        <div className="nav-links">
          <a href="#features">Features</a>
          <a href="/login">Login</a>
          <a className="nav-cta" href="/register">
            Sign up
          </a>
        </div>
      </nav>

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
            <a className="primary-button" href="/register">
              Start with CareDesk
            </a>
            <a className="secondary-button" href="/login">
              Log in
            </a>
          </div>
        </div>

        <div className="hero-visual" aria-label="Doctor consultation with CareDesk workflow preview">
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
          <a className="text-link" href="/register">
            Create account
          </a>
        </div>
        <img
          src="https://images.unsplash.com/photo-1584982751601-97dcc096659c?auto=format&fit=crop&fm=jpg&q=80&w=1200"
          alt="Healthcare team reviewing patient information on a tablet"
        />
      </section>
    </main>
  )
}
