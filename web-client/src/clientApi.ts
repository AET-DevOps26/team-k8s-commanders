import { API_URL } from './config'
import type { components } from './api'

export type AuthSession = components['schemas']['AuthSession']
export type LoginRequest = components['schemas']['LoginRequest']
export type RegisterRequest = components['schemas']['RegisterRequest']
export type UserProfile = components['schemas']['UserProfile']
export type Appointment = components['schemas']['Appointment']
export type AppointmentCreate = components['schemas']['AppointmentCreate']
export type AppointmentRescheduleRequest =
  components['schemas']['AppointmentRescheduleRequest']
export type PaginatedUserProfileResponse =
  components['schemas']['PaginatedUserProfileResponse']
export type PasswordChangeRequest = components['schemas']['PasswordChangeRequest']
export type Schedule = components['schemas']['Schedule']
export type ScheduleSlot = components['schemas']['ScheduleSlot']
export type VisitHistory = components['schemas']['VisitHistory']
export type PaginatedAppointmentResponse =
  components['schemas']['PaginatedAppointmentResponse']

type RequestOptions = {
  method?: string
  token?: string
  body?: unknown
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    method: options.method ?? 'GET',
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

async function getErrorMessage(response: Response) {
  const fallback = `Request failed with status ${response.status}`

  try {
    const payload = (await response.json()) as {
      title?: string
      detail?: string
      message?: string
    }

    return payload.detail ?? payload.message ?? payload.title ?? fallback
  } catch {
    return fallback
  }
}

export function login(payload: LoginRequest) {
  return request<AuthSession>('/auth/login', {
    method: 'POST',
    body: payload,
  })
}

export function registerPatient(payload: RegisterRequest) {
  return request<AuthSession>('/auth/register', {
    method: 'POST',
    body: {
      ...payload,
      role: 'PATIENT',
    },
  })
}

export function logout(token: string) {
  return request<void>('/auth/logout', {
    method: 'POST',
    token,
  })
}

export function getPatientProfile(patientId: string, token: string) {
  return request<UserProfile>(`/patients/${patientId}`, { token })
}

export function getPatientAppointments(patientId: string, token: string) {
  return request<PaginatedAppointmentResponse>(
    `/patients/${patientId}/appointments`,
    { token },
  )
}

export function getPatientVisitHistory(patientId: string, token: string) {
  return request<VisitHistory>(`/patients/${patientId}/visit-history`, {
    token,
  })
}

export function getUserProfile(userId: string, token: string) {
  return request<UserProfile>(`/users/${userId}`, { token })
}

export function updateUserProfile(userId: string, token: string, payload: UserProfile) {
  return request<UserProfile>(`/users/${userId}`, {
    method: 'PUT',
    token,
    body: payload,
  })
}

export function changeUserPassword(
  userId: string,
  token: string,
  payload: PasswordChangeRequest,
) {
  return request<void>(`/users/${userId}/password`, {
    method: 'PUT',
    token,
    body: payload,
  })
}

export function listDoctors(
  token: string,
  params: { q?: string; specialization?: string; page?: number; size?: number } = {},
) {
  const search = new URLSearchParams()

  if (params.q) {
    search.set('q', params.q)
  }

  if (params.specialization) {
    search.set('specialization', params.specialization)
  }

  search.set('page', String(params.page ?? 0))
  search.set('size', String(params.size ?? 20))

  return request<PaginatedUserProfileResponse>(`/doctors?${search.toString()}`, {
    token,
  })
}

export function getDoctorSchedule(doctorId: string, token: string) {
  return request<Schedule>(`/doctors/${doctorId}/schedule`, { token })
}

export function bookAppointment(token: string, payload: AppointmentCreate) {
  return request<Appointment>('/appointments', {
    method: 'POST',
    token,
    body: payload,
  })
}

export function rescheduleAppointment(
  token: string,
  appointmentId: string,
  payload: AppointmentRescheduleRequest,
) {
  return request<Appointment>(`/appointments/${appointmentId}`, {
    method: 'PUT',
    token,
    body: payload,
  })
}

export function cancelAppointment(token: string, appointmentId: string) {
  return request<Appointment>(`/appointments/${appointmentId}/cancel`, {
    method: 'POST',
    token,
  })
}
