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
export type UserCreate = components['schemas']['UserCreate']
export type UserStats = components['schemas']['UserStats']
export type UserRole = components['schemas']['UserRole']

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
      error?: string
    }

    return payload.detail ?? payload.message ?? payload.error ?? payload.title ?? fallback
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

// --- Admin user management (ADMIN role only) ---

export function listUsers(token: string, page = 0, size = 20) {
  return request<PaginatedUserProfileResponse>(
    `/users?page=${page}&size=${size}`,
    { token },
  )
}

export function getUserStats(token: string) {
  return request<UserStats>('/users/stats', { token })
}

export function createUser(payload: UserCreate, token: string) {
  return request<UserProfile>('/users', {
    method: 'POST',
    body: payload,
    token,
  })
}

export function replaceUser(
  userId: string,
  payload: UserProfile,
  token: string,
) {
  return request<UserProfile>(`/users/${userId}`, {
    method: 'PUT',
    body: payload,
    token,
  })
}

export function deactivateUser(userId: string, token: string) {
  return request<void>(`/users/${userId}`, {
    method: 'DELETE',
    token,
  })
}
