import { describe, expect, it } from 'vitest'
import {
  RequestError,
  getAppointmentNote,
  login,
  streamAiMessage,
} from './clientApi'
import { PATIENT_PASSWORD, patientUser } from './test/fixtures'
import { HttpResponse, http, server } from './test/server'

describe('request error handling', () => {
  it('throws a RequestError carrying the HTTP status', async () => {
    await expect(
      login({ email: 'wrong@example.com', password: 'nope-nope-nope' }),
    ).rejects.toMatchObject({ name: 'RequestError', status: 401 })
  })

  it('prefers the problem-detail message from the response body', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json({ detail: 'Account is locked' }, { status: 423 }),
      ),
    )

    await expect(
      login({ email: patientUser.email, password: PATIENT_PASSWORD }),
    ).rejects.toThrow('Account is locked')
  })

  it('falls back to a generic message for non-JSON error bodies', async () => {
    server.use(
      http.post(
        '*/api/v1/auth/login',
        () => new HttpResponse('boom', { status: 500 }),
      ),
    )

    await expect(
      login({ email: patientUser.email, password: PATIENT_PASSWORD }),
    ).rejects.toThrow('Request failed with status 500')
  })
})

describe('getAppointmentNote', () => {
  it('returns null when the note does not exist yet (404)', async () => {
    server.use(
      http.get('*/api/v1/appointments/:id/note', () =>
        HttpResponse.json({ detail: 'Not found' }, { status: 404 }),
      ),
    )

    await expect(getAppointmentNote('any-id', 'token')).resolves.toBeNull()
  })

  it('returns null for an empty note (204)', async () => {
    server.use(
      http.get(
        '*/api/v1/appointments/:id/note',
        () => new HttpResponse(null, { status: 204 }),
      ),
    )

    await expect(getAppointmentNote('any-id', 'token')).resolves.toBeNull()
  })

  it('rethrows other errors', async () => {
    server.use(
      http.get('*/api/v1/appointments/:id/note', () =>
        HttpResponse.json({ detail: 'Forbidden' }, { status: 403 }),
      ),
    )

    await expect(getAppointmentNote('any-id', 'token')).rejects.toBeInstanceOf(
      RequestError,
    )
  })
})

describe('streamAiMessage', () => {
  it('dispatches sources, token, and done SSE events', async () => {
    const sse = [
      'event: sources\ndata: ["note-1","note-2"]\n\n',
      'event: token\ndata: "Hello"\n\n',
      'event: token\ndata: " world"\n\n',
      'event: done\ndata: {}\n\n',
    ].join('')

    server.use(
      http.post(
        '*/api/v1/ai/sessions/:id/messages',
        () =>
          new HttpResponse(sse, {
            headers: { 'Content-Type': 'text/event-stream' },
          }),
      ),
    )

    const tokens: string[] = []
    let sources: string[] = []
    let done = false

    await streamAiMessage('session-1', 'What happened?', 'token', {
      onSources: (value) => {
        sources = value
      },
      onToken: (token) => tokens.push(token),
      onDone: () => {
        done = true
      },
    })

    expect(sources).toEqual(['note-1', 'note-2'])
    expect(tokens.join('')).toBe('Hello world')
    expect(done).toBe(true)
  })

  it('surfaces stream errors as RequestError', async () => {
    server.use(
      http.post(
        '*/api/v1/ai/sessions/:id/messages',
        () =>
          new HttpResponse(
            'event: error\ndata: {"detail":"Model unavailable"}\n\n',
            { headers: { 'Content-Type': 'text/event-stream' } },
          ),
      ),
    )

    await expect(
      streamAiMessage('session-1', 'query', 'token'),
    ).rejects.toThrow('Model unavailable')
  })
})
