import { useMemo, useState } from 'react'
import { formatAppointmentDate } from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'
import type { PatientDirectoryItem, PatientSummary } from './doctorUtils'

type PatientDirectoryProps = {
  summaries: PatientSummary[]
  queue: PatientDirectoryItem[]
  selectedPatientId: string | null
  onSelectPatient: (patientId: string) => void
}

export function PatientDirectory({
  summaries,
  queue,
  selectedPatientId,
  onSelectPatient,
}: PatientDirectoryProps) {
  const [query, setQuery] = useState('')

  const visibleItems = useMemo(() => {
    const trimmed = query.trim().toLowerCase()

    if (!trimmed) {
      return queue
    }

    return summaries
      .filter(
        ({ profile }) =>
          profile.name.toLowerCase().includes(trimmed) ||
          profile.email.toLowerCase().includes(trimmed),
      )
      .map<PatientDirectoryItem>((summary) => ({
        summary,
        appointment: summary.nextAppointment ?? summary.lastAppointment,
        timing: summary.nextAppointment
          ? 'upcoming'
          : summary.lastAppointment
            ? 'past'
            : 'none',
      }))
  }, [query, queue, summaries])

  return (
    <aside className="dashboard-panel patient-directory">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Patients</p>
          <h2>Patient queue</h2>
        </div>
      </div>

      <input
        className="search-input"
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search patient"
        type="search"
        value={query}
      />

      {visibleItems.length ? (
        <div className="patient-directory-list">
          {visibleItems.map(({ summary, appointment, timing }) => {
            const { profile, openAppointments, appointments } = summary

            return (
              <button
                className={
                  profile.id === selectedPatientId
                    ? 'patient-directory-card active'
                    : 'patient-directory-card'
                }
                key={profile.id}
                onClick={() => onSelectPatient(profile.id)}
                onMouseDown={(event) => event.preventDefault()}
                type="button"
              >
                <span className="patient-card-name">{profile.name}</span>
                <span>{profile.email}</span>
                <small>
                  {openAppointments} open · {appointments.length} total
                </small>
                <strong>
                  {timing === 'upcoming' && appointment
                    ? `Next · ${formatAppointmentDate(appointment.dateTime)}`
                    : timing === 'past' && appointment
                      ? `Last visit · ${formatAppointmentDate(appointment.dateTime)}`
                      : 'No appointments'}
                </strong>
              </button>
            )
          })}
        </div>
      ) : (
        <EmptyPanel text="No patients match this search." />
      )}
    </aside>
  )
}
