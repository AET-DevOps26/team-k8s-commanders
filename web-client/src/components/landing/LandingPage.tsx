import type { AuthSession } from '../../clientApi'
import { features, workflowStats } from '../../constants/landing'
import type { NavigateHandler } from '../../types/route'
import { ShellNav } from '../layout/ShellNav'
import { AppLink } from '../ui/AppLink'
import { FeatureIcon } from '../ui/FeatureIcon'

type LandingPageProps = {
  session: AuthSession | null
  onNavigate: NavigateHandler
  onLogout: () => void
}

export function LandingPage({ session, onNavigate, onLogout }: LandingPageProps) {
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
