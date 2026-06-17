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
export type ScheduleSlotCreate = components['schemas']['ScheduleSlotCreate']
export type VisitHistory = components['schemas']['VisitHistory']
export type PaginatedAppointmentResponse =
  components['schemas']['PaginatedAppointmentResponse']
export type ClinicalNote = components['schemas']['ClinicalNote']
export type ClinicalNoteInput = components['schemas']['ClinicalNoteInput']
export type Diagnosis = components['schemas']['Diagnosis']
export type AISession = components['schemas']['AISession']
export type AISessionCreateRequest =
  components['schemas']['AISessionCreateRequest']
export type AISessionSummary = components['schemas']['AISessionSummary']
export type AIMessage = components['schemas']['AIMessage']
export type AIMessageResponse = components['schemas']['AIMessageResponse']
export type PaginatedAISessionResponse =
  components['schemas']['PaginatedAISessionResponse']
export type UserCreate = components['schemas']['UserCreate']
export type UserStats = components['schemas']['UserStats']
export type UserRole = components['schemas']['UserRole']

/** Legacy shape kept for the doctor dashboard; maps to the sessions API. */
export type AIQueryRequest = {
  patientId?: string
  appointmentId?: string
  query: string
}

export type AIQueryResponse = AIMessageResponse

export type AIStreamEventHandlers = {
  onSources?: (sources: string[]) => void
  onToken?: (token: string) => void
  onDone?: () => void
}

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

export function listUsers(token: string, page = 0, size = 100) {
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

// --- Doctor dashboard ---

export function getDoctorAppointments(token: string, size = 100) {
  return request<PaginatedAppointmentResponse>(`/appointments?size=${size}`, {
    token,
  })
}

/**
 * Reads the clinical note for an appointment. Returns null when none has been
 * written yet. Older API versions answered 404; current notes-service answers
 * 204 so the browser console stays clean for normal empty notes.
 */
export async function getAppointmentNote(appointmentId: string, token: string) {
  try {
    const note = await request<ClinicalNote | undefined>(`/appointments/${appointmentId}/note`, {
      token,
    })
    return note ?? null
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

/** One-shot AI query via a transient session (doctor dashboard compatibility). */
export async function queryAi(payload: AIQueryRequest, token: string) {
  const session = await request<AISession>('/ai/sessions', {
    method: 'POST',
    body: {
      patientId: payload.patientId,
      appointmentId: payload.appointmentId,
    },
    token,
  })

  return request<AIMessageResponse>(`/ai/sessions/${session.id}/messages`, {
    method: 'POST',
    body: { query: payload.query },
    token,
  })
}

export function listAiSessions(token: string, page = 0, size = 100) {
  return request<PaginatedAISessionResponse>(
    `/ai/sessions?page=${page}&size=${size}`,
    { token },
  )
}

export function createAiSession(payload: AISessionCreateRequest, token: string) {
  return request<AISession>('/ai/sessions', {
    method: 'POST',
    body: payload,
    token,
  })
}

export function getAiSession(sessionId: string, token: string) {
  return request<AISession>(`/ai/sessions/${sessionId}`, { token })
}

export function deleteAiSession(sessionId: string, token: string) {
  return request<void>(`/ai/sessions/${sessionId}`, {
    method: 'DELETE',
    token,
  })
}

export function sendAiMessage(sessionId: string, query: string, token: string) {
  return request<AIMessageResponse>(`/ai/sessions/${sessionId}/messages`, {
    method: 'POST',
    body: { query },
    token,
  })
}

export async function streamAiMessage(
  sessionId: string,
  query: string,
  token: string,
  handlers: AIStreamEventHandlers = {},
) {
  const response = await fetch(`${API_URL}/ai/sessions/${sessionId}/messages`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ query }),
  })

  if (!response.ok) {
    throw new RequestError(response.status, await getErrorMessage(response))
  }

  if (!response.body) {
    throw new RequestError(response.status, 'Streaming response body is unavailable')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done })
    buffer = buffer.replace(/\r\n/g, '\n')

    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''

    for (const eventBlock of events) {
      handleAiSseEvent(eventBlock, handlers)
    }

    if (done) {
      if (buffer.trim()) {
        handleAiSseEvent(buffer, handlers)
      }
      break
    }
  }
}

function handleAiSseEvent(
  eventBlock: string,
  handlers: AIStreamEventHandlers,
) {
  const lines = eventBlock.split('\n')
  const event = lines
    .find((line) => line.startsWith('event:'))
    ?.slice('event:'.length)
    .trim()
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trimStart())
    .join('\n')
  const payload = data ? JSON.parse(data) : null

  if (event === 'sources') {
    handlers.onSources?.(Array.isArray(payload) ? payload : [])
    return
  }

  if (event === 'token') {
    handlers.onToken?.(typeof payload === 'string' ? payload : '')
    return
  }

  if (event === 'done') {
    handlers.onDone?.()
    return
  }

  if (event === 'error') {
    throw new RequestError(500, payload?.detail ?? 'AI stream failed')
  }
}

// --- Account self-service ---
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

// --- Doctors and appointments ---

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

export function createDoctorScheduleSlot(
  doctorId: string,
  token: string,
  payload: ScheduleSlotCreate,
) {
  return request<ScheduleSlot>(`/doctors/${doctorId}/schedule`, {
    method: 'POST',
    token,
    body: payload,
  })
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
