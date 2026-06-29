import type { Appointment } from '../../clientApi'
import { formatAppointmentDate } from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'
import { AppointmentStatusPill } from './AppointmentStatusPill'

type PatientAppointmentTimelineProps = {
  appointments: Appointment[]
  selectedAppointmentId: string | null
  onSelectAppointment: (appointmentId: string) => void
}

export function PatientAppointmentTimeline({
  appointments,
  selectedAppointmentId,
  onSelectAppointment,
}: PatientAppointmentTimelineProps) {
  if (!appointments.length) {
    return <EmptyPanel text="No appointments for this patient." />
  }

  return (
    <div className="patient-timeline">
      {appointments.map((appointment) => (
        <button
          className={
            appointment.id === selectedAppointmentId
              ? 'patient-timeline-item active'
              : 'patient-timeline-item'
          }
          key={appointment.id}
          onClick={() => onSelectAppointment(appointment.id)}
          type="button"
        >
          <span>{formatAppointmentDate(appointment.dateTime)}</span>
          <strong>{appointment.reason ?? 'No reason provided'}</strong>
          <AppointmentStatusPill status={appointment.status} />
        </button>
      ))}
    </div>
  )
}
