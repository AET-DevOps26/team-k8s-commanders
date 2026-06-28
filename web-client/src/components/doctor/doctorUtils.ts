import type { Appointment, UserProfile } from '../../clientApi'

export type PatientSummary = {
  profile: UserProfile
  appointments: Appointment[]
  nextAppointment: Appointment | null
  lastAppointment: Appointment | null
  openAppointments: number
}

export type PatientDirectoryItem = {
  summary: PatientSummary
  appointment: Appointment | null
  timing: 'upcoming' | 'past' | 'none'
}

export function isUpcomingAppointment(appointment: Appointment, now = Date.now()) {
  return (
    appointment.status !== 'CANCELLED' &&
    appointment.status !== 'COMPLETED' &&
    new Date(appointment.dateTime).getTime() >= now
  )
}

export function isPastAppointment(appointment: Appointment, now = Date.now()) {
  return (
    appointment.status !== 'CANCELLED' &&
    new Date(appointment.dateTime).getTime() < now
  )
}

export function isToday(value: string) {
  const date = new Date(value)
  const now = new Date()

  return (
    date.getUTCFullYear() === now.getUTCFullYear() &&
    date.getUTCMonth() === now.getUTCMonth() &&
    date.getUTCDate() === now.getUTCDate()
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
  now = Date.now(),
) {
  return patients
    .map<PatientSummary>((profile) => {
      const patientAppointments = appointments
        .filter((appointment) => appointment.patientId === profile.id)
        .sort(byDateDesc)
      const upcoming = patientAppointments
        .filter((appointment) => isUpcomingAppointment(appointment, now))
        .sort(byDateAsc)
      const past = patientAppointments.filter((appointment) =>
        isPastAppointment(appointment, now),
      )

      return {
        profile,
        appointments: patientAppointments,
        nextAppointment: upcoming[0] ?? null,
        lastAppointment: past[0] ?? null,
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

export function buildPatientDirectory(
  summaries: PatientSummary[],
  upcomingLimit = 3,
): PatientDirectoryItem[] {
  const upcoming = summaries
    .filter(
      (summary): summary is PatientSummary & { nextAppointment: Appointment } =>
        Boolean(summary.nextAppointment),
    )
    .slice(0, upcomingLimit)
    .map((summary) => ({
      summary,
      appointment: summary.nextAppointment,
      timing: 'upcoming' as const,
    }))
  const upcomingPatientIds = new Set(
    upcoming.map(({ summary }) => summary.profile.id),
  )
  const latestPast = summaries
    .filter(
      (summary): summary is PatientSummary & { lastAppointment: Appointment } =>
        Boolean(summary.lastAppointment) &&
        !upcomingPatientIds.has(summary.profile.id),
    )
    .sort((first, second) =>
      byDateDesc(first.lastAppointment, second.lastAppointment),
    )[0]

  if (!latestPast) {
    return upcoming
  }

  return [
    ...upcoming,
    {
      summary: latestPast,
      appointment: latestPast.lastAppointment,
      timing: 'past',
    },
  ]
}

export function patientName(patientId: string, users: Map<string, UserProfile>) {
  return users.get(patientId)?.name ?? patientId
}
