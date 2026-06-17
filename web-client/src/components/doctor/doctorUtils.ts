import type { Appointment, UserProfile } from '../../clientApi'

export type PatientSummary = {
  profile: UserProfile
  appointments: Appointment[]
  nextAppointment: Appointment | null
  lastAppointment: Appointment | null
  openAppointments: number
}

export function isUpcomingAppointment(appointment: Appointment, now = Date.now()) {
  return (
    appointment.status !== 'CANCELLED' &&
    appointment.status !== 'COMPLETED' &&
    new Date(appointment.dateTime).getTime() >= now
  )
}

export function isToday(value: string) {
  const date = new Date(value)
  const now = new Date()

  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  )
}

export function byDateAsc(first: Appointment, second: Appointment) {
  return new Date(first.dateTime).getTime() - new Date(second.dateTime).getTime()
}

export function byDateDesc(first: Appointment, second: Appointment) {
  return new Date(second.dateTime).getTime() - new Date(first.dateTime).getTime()
}

export function buildPatientSummaries(
  patients: UserProfile[],
  appointments: Appointment[],
) {
  return patients
    .map<PatientSummary>((profile) => {
      const patientAppointments = appointments
        .filter((appointment) => appointment.patientId === profile.id)
        .sort(byDateDesc)
      const upcoming = patientAppointments
        .filter((appointment) => isUpcomingAppointment(appointment))
        .sort(byDateAsc)

      return {
        profile,
        appointments: patientAppointments,
        nextAppointment: upcoming[0] ?? null,
        lastAppointment: patientAppointments[0] ?? null,
        openAppointments: upcoming.length,
      }
    })
    .sort((first, second) => {
      if (first.nextAppointment && second.nextAppointment) {
        return byDateAsc(first.nextAppointment, second.nextAppointment)
      }
      if (first.nextAppointment) {
        return -1
      }
      if (second.nextAppointment) {
        return 1
      }
      return first.profile.name.localeCompare(second.profile.name)
    })
}

export function patientName(patientId: string, users: Map<string, UserProfile>) {
  return users.get(patientId)?.name ?? patientId
}
