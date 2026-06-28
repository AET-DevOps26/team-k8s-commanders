import { useMemo, useState } from 'react'
import type { Appointment, UserProfile } from '../../clientApi'
import { EmptyPanel } from '../ui/EmptyPanel'
import { DoctorAppointmentRow } from './DoctorAppointmentRow'
import { byDateAsc, byDateDesc, isToday, isUpcomingAppointment, patientName } from './doctorUtils'

type ScheduleFilter = 'today' | 'upcoming' | 'history'

type DoctorSchedulePanelProps = {
  appointments: Appointment[]
  users: Map<string, UserProfile>
  onOpenPatient: (patientId: string) => void
}

export function DoctorSchedulePanel({
  appointments,
  users,
  onOpenPatient,
}: DoctorSchedulePanelProps) {
  const [filter, setFilter] = useState<ScheduleFilter>('today')

  const visibleAppointments = useMemo(() => {
    if (filter === 'today') {
      return appointments
        .filter((appointment) => isUpcomingAppointment(appointment))
        .filter((appointment) => isToday(appointment.dateTime))
        .sort(byDateAsc)
    }

    if (filter === 'upcoming') {
      return appointments.filter(isUpcomingAppointment).sort(byDateAsc)
    }

    return [...appointments].sort(byDateDesc)
  }, [appointments, filter])

  return (
    <article className="dashboard-panel doctor-schedule-panel" id="doctor-schedule">
      <div className="panel-header doctor-panel-header">
        <div>
          <p className="eyebrow">Appointments</p>
          <h2>Schedule</h2>
        </div>
        <div className="segmented-control" aria-label="Schedule filter">
          <button
            className={filter === 'today' ? 'active' : ''}
            onClick={() => setFilter('today')}
            type="button"
          >
            Today
          </button>
          <button
            className={filter === 'upcoming' ? 'active' : ''}
            onClick={() => setFilter('upcoming')}
            type="button"
          >
            Upcoming
          </button>
          <button
            className={filter === 'history' ? 'active' : ''}
            onClick={() => setFilter('history')}
            type="button"
          >
            All
          </button>
        </div>
      </div>

      {visibleAppointments.length ? (
        <div className="doctor-appointment-list">
          {visibleAppointments.map((appointment) => (
            <DoctorAppointmentRow
              appointment={appointment}
              key={appointment.id}
              patientName={patientName(appointment.patientId, users)}
              onOpenPatient={() => onOpenPatient(appointment.patientId)}
            />
          ))}
        </div>
      ) : (
        <EmptyPanel text="No appointments in this view." />
      )}
    </article>
  )
}
