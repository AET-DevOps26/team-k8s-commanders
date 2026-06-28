import type { Appointment } from '../../clientApi'
import { formatAppointmentDate } from '../../lib/dates'
import { AppointmentStatusPill } from './AppointmentStatusPill'

type DoctorAppointmentRowProps = {
  appointment: Appointment
  patientName: string
  onOpenPatient: () => void
}

export function DoctorAppointmentRow({
  appointment,
  patientName,
  onOpenPatient,
}: DoctorAppointmentRowProps) {
  return (
    <div className="doctor-appointment-row">
      <div>
        <strong>{formatAppointmentDate(appointment.dateTime)}</strong>
        <p>
          {patientName}
          {appointment.reason ? ` · ${appointment.reason}` : ''}
        </p>
      </div>
      <div className="doctor-row-actions">
        <AppointmentStatusPill status={appointment.status} />
        <button className="link-button" onClick={onOpenPatient} type="button">
          Open patient
        </button>
      </div>
    </div>
  )
}
