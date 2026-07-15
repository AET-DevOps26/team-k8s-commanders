import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AISession, AISessionSummary } from '../../clientApi'
import { HttpResponse, http, server } from '../../test/server'
import { DoctorAiAssistant } from './DoctorAiAssistant'
import { DoctorAiFloatingAssistant } from './DoctorAiFloatingAssistant'

const now = new Date().toISOString()

function summary(id: string, overrides: Partial<AISessionSummary> = {}): AISessionSummary {
  return {
    id,
    userId: 'doctor-id',
    title: `Chat ${id}`,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  }
}

function session(id: string, overrides: Partial<AISession> = {}): AISession {
  return {
    ...summary(id),
    messages: [],
    ...overrides,
  }
}

function page(content: AISessionSummary[]) {
  return {
    content,
    page: { page: 0, size: content.length, totalElements: content.length, totalPages: 1 },
  }
}

describe('doctor AI assistant', () => {
  afterEach(() => vi.restoreAllMocks())

  it('loads context sessions, renders Markdown, copies, switches, and deletes chats', async () => {
    const user = userEvent.setup()
    const first = summary('session-1', { appointmentId: 'appointment-1' })
    const second = summary('session-2')
    const deleted: string[] = []
    const clipboard = { writeText: vi.fn().mockResolvedValue(undefined) }
    Object.defineProperty(navigator, 'clipboard', { value: clipboard, configurable: true })

    server.use(
      http.get('*/api/v1/ai/sessions', () => HttpResponse.json(page([first, second]))),
      http.get('*/api/v1/ai/sessions/session-1', () =>
        HttpResponse.json(session('session-1', {
          appointmentId: 'appointment-1',
          messages: [{
            id: 'message-1',
            role: 'assistant',
            content: '## Findings\n\n- Stable\n\n[Source](https://example.com)',
            sources: ['patient record'],
            createdAt: now,
          }],
        })),
      ),
      http.get('*/api/v1/ai/sessions/session-2', () =>
        HttpResponse.json(session('session-2', {
          messages: [{ id: 'message-2', role: 'assistant', content: 'Second chat', createdAt: now }],
        })),
      ),
      http.delete('*/api/v1/ai/sessions/:sessionId', ({ params }) => {
        deleted.push(String(params.sessionId))
        return new HttpResponse(null, { status: 204 })
      }),
    )

    render(
      <DoctorAiAssistant
        appointmentId="appointment-1"
        contextKey="ai-test-load"
        token="token"
      />,
    )

    expect(await screen.findByRole('heading', { name: 'Findings' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Source' })).toHaveAttribute('target', '_blank')
    expect(screen.getByText('patient record')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Copy message' }))
    expect(clipboard.writeText).toHaveBeenCalledWith(expect.stringContaining('Findings'))
    expect(screen.getByRole('button', { name: 'Message copied' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Chat session-2/ }))
    expect(await screen.findByText('Second chat')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    await waitFor(() => expect(deleted).toEqual(['session-2']))
    expect(await screen.findByRole('heading', { name: 'Findings' })).toBeInTheDocument()
  })

  it('creates a chat, streams an answer, and refreshes saved messages', async () => {
    const user = userEvent.setup()
    const createdPayloads: unknown[] = []
    let sessionReads = 0
    server.use(
      http.get('*/api/v1/ai/sessions', () => HttpResponse.json(page([]))),
      http.post('*/api/v1/ai/sessions', async ({ request }) => {
        createdPayloads.push(await request.json())
        return HttpResponse.json(session('stream-session', { title: 'Medical assistant chat' }), {
          status: 201,
        })
      }),
      http.post('*/api/v1/ai/sessions/stream-session/messages', () =>
        new HttpResponse(
          'event: sources\ndata: ["guideline"]\n\n' +
          'event: token\ndata: "**Streamed** "\n\n' +
          'event: token\ndata: "answer"\n\n' +
          'event: done\ndata: null\n\n',
          { headers: { 'Content-Type': 'text/event-stream' } },
        ),
      ),
      http.get('*/api/v1/ai/sessions/stream-session', () => {
        sessionReads += 1
        return HttpResponse.json(session('stream-session', {
          title: 'Medical assistant chat',
          updatedAt: new Date(Date.now() + sessionReads * 1000).toISOString(),
          messages: [
            { id: 'user-message', role: 'user', content: 'Explain result', createdAt: now },
            {
              id: 'assistant-message',
              role: 'assistant',
              content: '**Streamed** answer',
              sources: ['guideline'],
              createdAt: now,
            },
          ],
        }))
      }),
    )

    render(
      <DoctorAiAssistant
        appointmentId={null}
        contextKey="ai-test-stream"
        title="Medical assistant"
        token="token"
      />,
    )
    expect(await screen.findByText('No saved AI chats for this context yet.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Ask about this context'), 'Explain result')
    await user.click(screen.getByRole('button', { name: 'Ask AI' }))

    expect(await screen.findByText('Streamed')).toBeInTheDocument()
    expect(screen.getByText('guideline')).toBeInTheDocument()
    expect(createdPayloads).toEqual([{ title: 'Medical assistant chat' }])
  })

  it('shows failed streams and retries same question', async () => {
    const user = userEvent.setup()
    let attempts = 0
    server.use(
      http.get('*/api/v1/ai/sessions', () => HttpResponse.json(page([]))),
      http.post('*/api/v1/ai/sessions', () =>
        HttpResponse.json(session('retry-session'), { status: 201 }),
      ),
      http.post('*/api/v1/ai/sessions/retry-session/messages', () => {
        attempts += 1
        if (attempts === 1) {
          return HttpResponse.json({ detail: 'AI offline' }, { status: 503 })
        }
        return new HttpResponse(
          'event: token\ndata: "Recovered"\n\nevent: done\ndata: null\n\n',
          { headers: { 'Content-Type': 'text/event-stream' } },
        )
      }),
      http.get('*/api/v1/ai/sessions/retry-session', () =>
        HttpResponse.json(session('retry-session', {
          messages: [{ id: 'recovered', role: 'assistant', content: 'Recovered', createdAt: now }],
        })),
      ),
    )
    render(
      <DoctorAiAssistant appointmentId={null} contextKey="ai-test-retry" token="token" />,
    )
    await screen.findByText('No saved AI chats for this context yet.')
    await user.type(screen.getByLabelText('Ask about this context'), 'Retry me')
    await user.click(screen.getByRole('button', { name: 'Ask AI' }))

    expect(await screen.findByText('AI assistant is unavailable. Please try again.'))
      .toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('Recovered')).toBeInTheDocument()
    expect(attempts).toBe(2)
  })

  it('recovers from session-list failures and reports create failures', async () => {
    const user = userEvent.setup()
    let listAttempts = 0
    server.use(
      http.get('*/api/v1/ai/sessions', () => {
        listAttempts += 1
        return listAttempts === 1
          ? HttpResponse.json({ detail: 'Unavailable' }, { status: 503 })
          : HttpResponse.json(page([]))
      }),
      http.post('*/api/v1/ai/sessions', () =>
        HttpResponse.json({ detail: 'Creation rejected' }, { status: 500 }),
      ),
    )
    render(
      <DoctorAiAssistant appointmentId={null} contextKey="ai-test-errors" token="token" />,
    )

    expect(await screen.findByText(/AI sessions could not be loaded/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('No saved AI chats for this context yet.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'New chat' }))
    expect(await screen.findByText(/AI session could not be created/)).toBeInTheDocument()
  })

  it('opens and closes floating assistant with button and Escape', async () => {
    const user = userEvent.setup()
    server.use(http.get('*/api/v1/ai/sessions', () => HttpResponse.json(page([]))))
    render(
      <DoctorAiFloatingAssistant
        contextKey="ai-test-floating"
        title="Medical assistant"
        token="token"
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Open doctor AI assistant' }))
    expect(screen.getByRole('dialog', { name: 'Doctor AI assistant' })).toBeInTheDocument()
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Doctor AI assistant' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Open doctor AI assistant' }))
    await user.click(screen.getByRole('button', { name: 'Close doctor AI assistant' }))
    expect(screen.queryByRole('dialog', { name: 'Doctor AI assistant' })).not.toBeInTheDocument()
  })
})
