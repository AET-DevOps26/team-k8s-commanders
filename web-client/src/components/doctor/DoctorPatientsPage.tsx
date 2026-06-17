import { useEffect, useMemo, useState } from 'react'
import type { Appointment, UserProfile } from '../../clientApi'
import { getDoctorAppointments, listUsers } from '../../clientApi'
import { userMessage } from '../../lib/messages'
import type { DoctorDashboardProps } from '../../types/route'
import { DoctorSubNav } from '../layout/DoctorSubNav'
import { ShellNav } from '../layout/ShellNav'
import { StatusPanel } from '../ui/StatusPanel'
import { PatientDirectory } from './PatientDirectory'
import { PatientWorkspace } from './PatientWorkspace'
import { buildPatientSummaries } from './doctorUtils'

export function DoctorPatientsPage({
  session,
  onLogout,
  onNavigate,
}: DoctorDashboardProps) {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [users, setUsers] = useState<UserProfile[]>([])
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const doctorId = session.user.id
  const token = session.accessToken

  useEffect(() => {
    let isActive = true

    async function loadPatients() {
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
      } catch {
        if (isActive) {
          setError(userMessage('Patient records could not be loaded. Please try again.'))
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadPatients()

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
    () => appointments.filter((appointment) => appointment.doctorId === doctorId),
    [appointments, doctorId],
  )

  const patientSummaries = useMemo(
    () => buildPatientSummaries(patients, doctorAppointments),
    [doctorAppointments, patients],
  )

  const selectedDirectoryProfile = useMemo(
    () => userMap.get(selectedPatientId ?? '') ?? null,
    [selectedPatientId, userMap],
  )

  useEffect(() => {
    if (!selectedPatientId && patientSummaries.length) {
      setSelectedPatientId(patientSummaries[0].profile.id)
    }
  }, [patientSummaries, selectedPatientId])

  if (session.user.role !== 'DOCTOR') {
    return (
      <main className="landing-page app-page">
        <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
        <section className="empty-state">
          <p className="eyebrow">Patient records</p>
          <h1>Doctor account required.</h1>
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
        <header className="patient-hero doctor-hero">
          <div>
            <p className="eyebrow">Doctor patients</p>
            <h1>Patient records</h1>
            <p>Individual clinical views with notes, timeline, and persistent AI chat.</p>
          </div>
        </header>

        <DoctorSubNav active="patients" onNavigate={onNavigate} />

        {isLoading && <StatusPanel title="Loading patient records" />}
        {error && <StatusPanel title="Patient API unavailable" text={error} />}

        {!isLoading && !error && (
          <section className="doctor-patient-page-layout">
            <PatientDirectory
              summaries={patientSummaries}
              selectedPatientId={selectedPatientId}
              onSelectPatient={setSelectedPatientId}
            />
            <PatientWorkspace
              directoryProfile={selectedDirectoryProfile}
              patientId={selectedPatientId}
              token={token}
            />
          </section>
        )}
      </section>
    </main>
  )
}
