import { API_URL } from './config'
import type { components } from './api'

export type AuthSession = components['schemas']['AuthSession']
export type LoginRequest = components['schemas']['LoginRequest']
export type RegisterRequest = components['schemas']['RegisterRequest']
export type UserProfile = components['schemas']['UserProfile']
export type Appointment = components['schemas']['Appointment']
export type VisitHistory = components['schemas']['VisitHistory']
export type PaginatedAppointmentResponse =
  components['schemas']['PaginatedAppointmentResponse']
export type PaginatedUserProfileResponse =
  components['schemas']['PaginatedUserProfileResponse']
export type ClinicalNote = components['schemas']['ClinicalNote']
export type ClinicalNoteInput = components['schemas']['ClinicalNoteInput']
export type Diagnosis = components['schemas']['Diagnosis']
export type AIQueryRequest = components['schemas']['AIQueryRequest']
export type AIQueryResponse = components['schemas']['AIQueryResponse']

type RequestOptions = {
  method?: string
  token?: string
  body?: unknown
}

/** Error carrying the HTTP status so callers can branch on it (e.g. 404). */
export class RequestError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'RequestError'
    this.status = status
  }
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
    throw new RequestError(response.status, await getErrorMessage(response))
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

export function listUsers(token: string, size = 100) {
  return request<PaginatedUserProfileResponse>(`/users?size=${size}`, { token })
}

export function getDoctorAppointments(token: string, size = 100) {
  return request<PaginatedAppointmentResponse>(`/appointments?size=${size}`, {
    token,
  })
}

/**
 * Reads the clinical note for an appointment. Returns null when none has been
 * written yet (the API answers 404 in that case).
 */
export async function getAppointmentNote(appointmentId: string, token: string) {
  try {
    return await request<ClinicalNote>(`/appointments/${appointmentId}/note`, {
      token,
    })
  } catch (error) {
    if (error instanceof RequestError && error.status === 404) {
      return null
    }
    throw error
  }
}

export function upsertAppointmentNote(
  appointmentId: string,
  input: ClinicalNoteInput,
  token: string,
) {
  return request<ClinicalNote>(`/appointments/${appointmentId}/note`, {
    method: 'PUT',
    body: input,
    token,
  })
}

export function queryAi(payload: AIQueryRequest, token: string) {
  return request<AIQueryResponse>('/ai/query', {
    method: 'POST',
    body: payload,
    token,
  })
}
