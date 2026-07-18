import { useEffect, useMemo, useState } from 'react'
import type { Appointment, UserProfile, VisitHistory } from '../../clientApi'
import {
  getPatientAppointments,
  getPatientVisitHistory,
  getUserProfile,
} from '../../clientApi'
import { formatAppointmentDate, isPastDateTime } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import type { PatientDashboardViewProps } from '../../types/route'
import {
  AppointmentFilterBar,
  type AppointmentSortOrder,
  type AppointmentStatusFilter,
} from '../appointments/AppointmentFilterBar'
import { AppointmentRow } from '../appointments/AppointmentRow'
import { PatientSubNav } from '../layout/PatientSubNav'
import { ShellNav } from '../layout/ShellNav'
import { EmptyPanel } from '../ui/EmptyPanel'
import { StatusPanel } from '../ui/StatusPanel'
import { SummaryCard } from '../ui/SummaryCard'

type PatientData = {
  profile: UserProfile
  appointments: Appointment[]
  visitHistory: VisitHistory
}

export function PatientDashboard({
  session,
  onLogout,
  onNavigate,
  bookingSuccess = false,
}: PatientDashboardViewProps) {
  const [patientData, setPatientData] = useState<PatientData | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)
  const [movingId, setMovingId] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<AppointmentStatusFilter>('ALL')
  const [sortOrder, setSortOrder] = useState<AppointmentSortOrder>('newest')
  const patientId = session.user.id

  const upcomingAppointments = useMemo(() => {
    if (!patientData) {
      return []
    }

    return patientData.appointments
      .filter((appointment) => appointment.status !== 'CANCELLED')
      .filter((appointment) => !isPastDateTime(appointment.dateTime))
      .sort(
        (first, second) =>
          new Date(first.dateTime).getTime() -
          new Date(second.dateTime).getTime(),
      )
  }, [patientData])

  const scheduleAppointments = useMemo(() => {
    if (!patientData) {
      return []
    }

    const filtered =
      statusFilter === 'ALL'
        ? patientData.appointments
        : patientData.appointments.filter(
            (appointment) => appointment.status === statusFilter,
          )

    return [...filtered].sort((first, second) => {
      const ascending =
        new Date(first.dateTime).getTime() - new Date(second.dateTime).getTime()
      return sortOrder === 'oldest' ? ascending : -ascending
    })
  }, [patientData, sortOrder, statusFilter])

  const pastVisits = useMemo(() => {
    if (!patientData) {
      return []
    }

    return patientData.visitHistory.appointments.filter(
      (appointment) =>
        appointment.status === 'COMPLETED' ||
        isPastDateTime(appointment.dateTime),
    )
  }, [patientData])

  const nextAppointment = useMemo(() => {
    return upcomingAppointments[0] ?? null
  }, [upcomingAppointments])

  const showBookingSuccess = bookingSuccess && !isLoading && Boolean(patientData)

  useEffect(() => {
    if (!showBookingSuccess) {
      return
    }

    document.getElementById('patient-schedule')?.scrollIntoView({ behavior: 'smooth' })
  }, [showBookingSuccess])

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
      } catch {
        if (isActive) {
          setError(
            userMessage('Your dashboard could not be loaded. Please try again in a moment.'),
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
        {showBookingSuccess && (
          <StatusPanel
            title="Appointment booked successfully."
            text="Your appointment is now listed in your schedule below."
          />
        )}

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

            <section className="dashboard-grid">
              <article className="dashboard-panel wide-panel" id="patient-schedule">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Appointments</p>
                    <h2>My schedule</h2>
                  </div>
                </div>
                {!movingId && (
                  <AppointmentFilterBar
                    statusFilter={statusFilter}
                    sortOrder={sortOrder}
                    onStatusFilterChange={setStatusFilter}
                    onSortOrderChange={setSortOrder}
                  />
                )}
                {scheduleAppointments.length ? (
                  <div
                    className={
                      movingId
                        ? 'appointment-list appointment-list-focused'
                        : 'appointment-list'
                    }
                  >
                    {scheduleAppointments
                      .filter((appointment) => !movingId || appointment.id === movingId)
                      .map((appointment) => (
                        <AppointmentRow
                          appointment={appointment}
                          key={appointment.id}
                          isMoving={movingId === appointment.id}
                          onMoveOpen={() => setMovingId(appointment.id)}
                          onMoveClose={() => setMovingId(null)}
                          onChanged={() => {
                            setMovingId(null)
                            setReloadKey((current) => current + 1)
                          }}
                          token={session.accessToken}
                        />
                      ))}
                  </div>
                ) : (
                  <EmptyPanel
                    text={
                      statusFilter === 'ALL'
                        ? 'No appointments booked yet.'
                        : 'No appointments match this filter.'
                    }
                  />
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
            </section>
          </>
        )}
      </section>
    </main>
  )
}
