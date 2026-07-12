import { useEffect, useMemo, useState } from 'react'
import type { Appointment, UserProfile } from '../../clientApi'
import { fetchAllAppointmentPages, fetchAllUserPages } from '../../clientApi'
import { userMessage } from '../../lib/messages'
import type { DoctorDashboardViewProps } from '../../types/route'
import { DoctorSubNav } from '../layout/DoctorSubNav'
import { ShellNav } from '../layout/ShellNav'
import { StatusPanel } from '../ui/StatusPanel'
import { SummaryCard } from '../ui/SummaryCard'
import { DoctorAiFloatingAssistant } from './DoctorAiFloatingAssistant'
import { DoctorSchedulePanel } from './DoctorSchedulePanel'
import { buildPatientSummaries, isToday, isUpcomingAppointment } from './doctorUtils'

export function DoctorDashboard({
  session,
  onLogout,
  onNavigate,
  bookingSuccess = false,
  onBookingSuccessAcknowledged,
}: DoctorDashboardViewProps) {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [users, setUsers] = useState<UserProfile[]>([])
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)
  const doctorId = session.user.id
  const token = session.accessToken

  useEffect(() => {
    let isActive = true

    async function loadDoctorData() {
      setLoading(true)
      setError('')

      try {
        const [loadedAppointments, loadedUsers] = await Promise.all([
          fetchAllAppointmentPages(token),
          fetchAllUserPages(token),
        ])

        if (isActive) {
          setAppointments(loadedAppointments)
          setUsers(loadedUsers)
        }
      } catch {
        if (isActive) {
          setError(userMessage('Doctor data could not be loaded. Please try again.'))
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
      appointments.filter((appointment) => appointment.doctorId === doctorId),
    [appointments, doctorId],
  )

  const upcomingAppointments = useMemo(
    () => doctorAppointments.filter(isUpcomingAppointment),
    [doctorAppointments],
  )

  const patientSummaries = useMemo(
    () => buildPatientSummaries(patients, doctorAppointments),
    [doctorAppointments, patients],
  )

  useEffect(() => {
    if (!bookingSuccess || isLoading) {
      return
    }

    setStatus('Appointment booked successfully.')
    onBookingSuccessAcknowledged?.()
  }, [bookingSuccess, isLoading, onBookingSuccessAcknowledged])

  useEffect(() => {
    if (!status || isLoading) {
      return
    }

    document.getElementById('doctor-schedule')?.scrollIntoView({ behavior: 'smooth' })
  }, [isLoading, status])

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

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell doctor-dashboard-shell">
        <DoctorSubNav active="dashboard" onNavigate={onNavigate} />
        <header className="patient-hero doctor-hero">
          <div>
            <p className="eyebrow">Doctor dashboard</p>
            <h1>{session.user.name}</h1>
            <p>
              Today at a glance, with patient records and availability
              kept on dedicated pages.
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
        {status && (
          <StatusPanel
            title={status}
            text="The appointment is now listed in your schedule below."
          />
        )}

        {!isLoading && !error && (
          <>
            <section className="patient-summary doctor-summary">
              <SummaryCard
                label="Upcoming"
                value={String(upcomingAppointments.length)}
                text="Scheduled appointments ahead"
              />
              <SummaryCard
                label="Today"
                value={String(
                  upcomingAppointments.filter((appointment) =>
                    isToday(appointment.dateTime),
                  ).length,
                )}
                text="Appointments today"
              />
              <SummaryCard
                label="Patients"
                value={String(patientSummaries.length)}
                text="Patients in your panel"
              />
            </section>

            <section className="doctor-action-row" aria-label="Doctor actions">
              <button
                className="primary-button"
                onClick={() => onNavigate('/doctor/book')}
                type="button"
              >
                Book for patient
              </button>
              <button
                className="secondary-button"
                onClick={() => onNavigate('/doctor/schedule')}
                type="button"
              >
                Manage availability
              </button>
              <button
                className="secondary-button"
                onClick={() => onNavigate('/doctor/patients')}
                type="button"
              >
                Open patient records
              </button>
            </section>

            <section className="doctor-overview-grid">
              <DoctorSchedulePanel
                appointments={doctorAppointments}
                users={userMap}
                onOpenPatient={() => onNavigate('/doctor/patients')}
              />
              <section className="dashboard-panel doctor-focus-panel">
                <div className="panel-header doctor-panel-header">
                  <div>
                    <p className="eyebrow">Focus</p>
                    <h2>Next steps</h2>
                  </div>
                </div>
                <div className="doctor-focus-list">
                  <button
                    className="doctor-focus-card"
                    onClick={() => onNavigate('/doctor/schedule')}
                    type="button"
                  >
                    <strong>Publish bookable time</strong>
                    <span>Create available slots without assigning patients.</span>
                  </button>
                  <button
                    className="doctor-focus-card"
                    onClick={() => onNavigate('/doctor/patients')}
                    type="button"
                  >
                    <strong>Continue patient work</strong>
                    <span>Open notes, timeline, and persistent AI chat.</span>
                  </button>
                </div>
              </section>
            </section>
          </>
        )}
      </section>
      <DoctorAiFloatingAssistant
        contextKey="doctor:general-medical"
        inputLabel="Ask a medical question"
        placeholder="Ask a general medical question or clarify clinical guidance."
        title="Medical assistant"
        token={token}
      />
    </main>
  )
}
