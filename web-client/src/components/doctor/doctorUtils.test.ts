import { describe, expect, it } from 'vitest'
import type { Appointment, UserProfile } from '../../clientApi'
import {
  buildPatientDirectory,
  buildPatientSummaries,
  isPastAppointment,
  isUpcomingAppointment,
  patientName,
} from './doctorUtils'

const NOW = Date.parse('2030-01-10T12:00:00Z')

const patients: UserProfile[] = [
  { id: 'patient-a', name: 'Anna', email: 'anna@example.com', role: 'PATIENT' },
  { id: 'patient-b', name: 'Berta', email: 'berta@example.com', role: 'PATIENT' },
  { id: 'patient-c', name: 'Clara', email: 'clara@example.com', role: 'PATIENT' },
]

function appointment(
  id: string,
  patientId: string,
  dateTime: string,
  status: Appointment['status'] = 'SCHEDULED',
): Appointment {
  return {
    id,
    patientId,
    doctorId: 'doctor',
    dateTime,
    status,
    duration: 30,
  }
}

describe('doctor appointment classification', () => {
  it('does not treat cancelled or completed future visits as upcoming', () => {
    const future = '2030-01-11T09:00:00Z'

    expect(isUpcomingAppointment(appointment('1', 'patient-a', future), NOW)).toBe(true)
    expect(
      isUpcomingAppointment(appointment('2', 'patient-a', future, 'CANCELLED'), NOW),
    ).toBe(false)
    expect(
      isUpcomingAppointment(appointment('3', 'patient-a', future, 'COMPLETED'), NOW),
    ).toBe(false)
  })

  it('keeps completed historical visits but excludes cancelled ones', () => {
    const past = '2030-01-09T09:00:00Z'

    expect(isPastAppointment(appointment('1', 'patient-a', past, 'COMPLETED'), NOW)).toBe(true)
    expect(isPastAppointment(appointment('2', 'patient-a', past, 'CANCELLED'), NOW)).toBe(false)
  })
})

describe('patient summaries', () => {
  const appointments = [
    appointment('a-next', 'patient-a', '2030-01-12T09:00:00Z'),
    appointment('a-last', 'patient-a', '2030-01-08T09:00:00Z', 'COMPLETED'),
    appointment('b-next', 'patient-b', '2030-01-11T09:00:00Z', 'RESCHEDULED'),
    appointment('b-later', 'patient-b', '2030-01-15T09:00:00Z'),
    appointment('c-last', 'patient-c', '2030-01-09T09:00:00Z', 'COMPLETED'),
  ]

  it('sorts patients by next visit and computes open and historical state', () => {
    const summaries = buildPatientSummaries(patients, appointments, NOW)

    expect(summaries.map(({ profile }) => profile.id)).toEqual([
      'patient-b',
      'patient-a',
      'patient-c',
    ])
    expect(summaries[0].nextAppointment?.id).toBe('b-next')
    expect(summaries[0].openAppointments).toBe(2)
    expect(summaries[1].lastAppointment?.id).toBe('a-last')
  })

  it('builds focused directory from upcoming patients plus latest past patient', () => {
    const directory = buildPatientDirectory(
      buildPatientSummaries(patients, appointments, NOW),
      1,
    )

    expect(directory.map(({ summary, timing }) => [summary.profile.id, timing])).toEqual([
      ['patient-b', 'upcoming'],
      ['patient-c', 'past'],
    ])
  })

  it('uses patient id when profile lookup is unavailable', () => {
    const users = new Map(patients.map((patient) => [patient.id, patient]))

    expect(patientName('patient-a', users)).toBe('Anna')
    expect(patientName('missing-patient', users)).toBe('missing-patient')
  })
})
