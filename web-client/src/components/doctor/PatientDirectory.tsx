import { useMemo, useState } from 'react'
import { formatAppointmentDate } from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'
import type { PatientSummary } from './doctorUtils'

type PatientDirectoryProps = {
  summaries: PatientSummary[]
  selectedPatientId: string | null
  onSelectPatient: (patientId: string) => void
}

export function PatientDirectory({
  summaries,
  selectedPatientId,
  onSelectPatient,
}: PatientDirectoryProps) {
  const [query, setQuery] = useState('')

  const matches = useMemo(() => {
    const trimmed = query.trim().toLowerCase()

    if (!trimmed) {
      return summaries
    }

    return summaries.filter(
      ({ profile }) =>
        profile.name.toLowerCase().includes(trimmed) ||
        profile.email.toLowerCase().includes(trimmed),
    )
  }, [query, summaries])

  return (
    <aside className="dashboard-panel patient-directory">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Patients</p>
          <h2>Panel</h2>
        </div>
      </div>

      <input
        className="search-input"
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search patient"
        type="search"
        value={query}
      />

      {matches.length ? (
        <div className="patient-directory-list">
          {matches.map(({ profile, nextAppointment, openAppointments, appointments }) => (
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
                {nextAppointment
                  ? formatAppointmentDate(nextAppointment.dateTime)
                  : 'No upcoming appointment'}
              </strong>
            </button>
          ))}
        </div>
      ) : (
        <EmptyPanel text="No patients match this search." />
      )}
    </aside>
  )
}
