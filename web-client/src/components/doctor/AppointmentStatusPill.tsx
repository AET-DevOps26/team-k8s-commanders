import type { Appointment } from '../../clientApi'

type AppointmentStatusPillProps = {
  status: Appointment['status']
}

export function AppointmentStatusPill({ status }: AppointmentStatusPillProps) {
  return (
    <span className={`appointment-status appointment-status-${status.toLowerCase()}`}>
      {status}
    </span>
  )
}
