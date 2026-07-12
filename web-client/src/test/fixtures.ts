import type {
  Appointment,
  AuthSession,
  Schedule,
  ScheduleSlot,
  UserProfile,
  VisitHistory,
} from '../clientApi'
import { SESSION_KEY } from '../constants/session'

export const PATIENT_PASSWORD = 'correct-horse-battery'

export const patientUser: UserProfile = {
  id: '11111111-1111-4111-8111-111111111111',
  name: 'Anna Beispiel',
  email: 'anna@example.com',
  role: 'PATIENT',
  phoneNumber: '+49 170 1234567',
  dateOfBirth: '1990-04-12',
}

export const patientSession: AuthSession = {
  accessToken: 'test-access-token',
  user: patientUser,
}

export const doctorUser: UserProfile = {
  id: '22222222-2222-4222-8222-222222222222',
  name: 'Dr. Julia Weber',
  email: 'julia.weber@caredesk.example',
  role: 'DOCTOR',
  specialization: 'General Medicine',
}

export function isoDaysFromNow(days: number, hour = 10) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  date.setHours(hour, 0, 0, 0)
  return date.toISOString()
}

export const upcomingAppointment: Appointment = {
  id: '33333333-3333-4333-8333-333333333333',
  patientId: patientUser.id,
  doctorId: doctorUser.id,
  dateTime: isoDaysFromNow(7),
  status: 'SCHEDULED',
  duration: 30,
  reason: 'Annual check-up',
}

export const pastAppointment: Appointment = {
  id: '44444444-4444-4444-8444-444444444444',
  patientId: patientUser.id,
  doctorId: doctorUser.id,
  dateTime: isoDaysFromNow(-30),
  status: 'COMPLETED',
  duration: 30,
  reason: 'Flu symptoms',
}

export const visitHistory: VisitHistory = {
  patientId: patientUser.id,
  appointments: [pastAppointment],
}

export const availableSlots: ScheduleSlot[] = [
  {
    id: '77777777-7777-4777-8777-777777777777',
    startAt: isoDaysFromNow(3, 9),
    endAt: isoDaysFromNow(3, 10),
    available: true,
  },
  {
    id: '88888888-8888-4888-8888-888888888888',
    startAt: isoDaysFromNow(4, 14),
    endAt: isoDaysFromNow(4, 15),
    available: true,
  },
]

export const doctorSchedule: Schedule = {
  doctorId: doctorUser.id,
  slots: availableSlots,
}

export function paginated<T>(content: T[]) {
  return {
    content,
    page: {
      page: 0,
      size: content.length,
      totalElements: content.length,
      totalPages: 1,
    },
  }
}

export function seedStoredSession(session: AuthSession = patientSession) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}
