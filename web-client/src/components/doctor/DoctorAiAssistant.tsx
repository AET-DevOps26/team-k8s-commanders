import { useEffect, useMemo, useState, type FormEvent } from 'react'
import type { AIMessage, AISession, AISessionSummary } from '../../clientApi'
import {
  createAiSession,
  deleteAiSession,
  getAiSession,
  listAiSessions,
  streamAiMessage,
} from '../../clientApi'
import { formatAppointmentDate } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import { EmptyPanel } from '../ui/EmptyPanel'
import { StatusPanel } from '../ui/StatusPanel'

type DoctorAiAssistantProps = {
  patientId?: string | null
  appointmentId: string | null
  patientName?: string
  token: string
  contextKey?: string
  title?: string
  prompts?: string[]
  inputLabel?: string
  placeholder?: string
}

type StreamingReply = {
  id: string
  question: string
  answer: string
  sources: string[]
  failed?: boolean
}

type RenderedAIMessage = AIMessage & {
  failed?: boolean
  retryQuestion?: string
}

const quickPrompts = [
  'Summarize recent visits and open risks.',
  'What changed since the last appointment?',
  'Draft questions for the next consultation.',
]

type DoctorAiState = {
  activeSession: AISession | null
  error: string
  isLoadingSessions: boolean
  isStreaming: boolean
  loadedFor: string | null
  query: string
  sessions: AISessionSummary[]
  streamingReply: StreamingReply | null
}

const defaultDoctorAiState = (): DoctorAiState => ({
  activeSession: null,
  error: '',
  isLoadingSessions: true,
  isStreaming: false,
  loadedFor: null,
  query: '',
  sessions: [],
  streamingReply: null,
})

const doctorAiStates = new Map<string, DoctorAiState>()
const doctorAiListeners = new Map<string, Set<() => void>>()

function getDoctorAiState(contextKey: string) {
  const existing = doctorAiStates.get(contextKey)
  if (existing) {
    return existing
  }

  const next = defaultDoctorAiState()
  doctorAiStates.set(contextKey, next)
  return next
}

function updateDoctorAiState(
  contextKey: string,
  updater: (state: DoctorAiState) => DoctorAiState,
) {
  const next = updater(getDoctorAiState(contextKey))
  doctorAiStates.set(contextKey, next)
  doctorAiListeners.get(contextKey)?.forEach((listener) => listener())
}

function subscribeDoctorAiState(contextKey: string, listener: () => void) {
  const listeners = doctorAiListeners.get(contextKey) ?? new Set<() => void>()
  listeners.add(listener)
  doctorAiListeners.set(contextKey, listeners)

  return () => {
    listeners.delete(listener)
  }
}

export function DoctorAiAssistant({
  patientId,
  appointmentId,
  patientName = 'Doctor AI',
  token,
  contextKey,
  title = patientName,
  prompts = quickPrompts,
  inputLabel = 'Ask about this context',
  placeholder = 'Summarize the current context and flag anything I should review.',
}: DoctorAiAssistantProps) {
  const resolvedContextKey =
    contextKey ??
    (patientId
      ? `patient:${patientId}:appointment:${appointmentId ?? 'none'}`
      : 'doctor:general')
  const [state, setState] = useState(() => getDoctorAiState(resolvedContextKey))
  const [loadAttempt, setLoadAttempt] = useState(0)
  const {
    activeSession,
    error,
    isLoadingSessions,
    isStreaming,
    loadedFor,
    query,
    sessions,
    streamingReply,
  } = state

  useEffect(
    () =>
      subscribeDoctorAiState(resolvedContextKey, () => {
        setState(getDoctorAiState(resolvedContextKey))
      }),
    [resolvedContextKey],
  )

  useEffect(() => {
    setState(getDoctorAiState(resolvedContextKey))
  }, [resolvedContextKey])

  useEffect(() => {
    const loadKey = `${token}:${patientId ?? ''}:${appointmentId ?? ''}`
    const currentState = getDoctorAiState(resolvedContextKey)
    if (currentState.loadedFor === loadKey || currentState.isStreaming) {
      return
    }

    async function loadSessions() {
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        error: '',
        isLoadingSessions: true,
        loadedFor: loadKey,
      }))

      try {
        const response = await listAiSessions(token, 0, 100)
        const contextSessions = response.content.filter(
          (session) =>
            patientId ? session.patientId === patientId : !session.patientId,
        )

        const preferredSession =
          contextSessions.find(
            (session) => appointmentId && session.appointmentId === appointmentId,
          ) ??
          contextSessions.find((session) => !session.appointmentId) ??
          contextSessions[0] ??
          null

        let loadedSession: AISession | null = null
        if (preferredSession) {
          loadedSession = await getAiSession(preferredSession.id, token)
        }

        updateDoctorAiState(resolvedContextKey, (current) => ({
          ...current,
          activeSession: loadedSession,
          error: '',
          isLoadingSessions: false,
          sessions: contextSessions,
        }))
      } catch {
        updateDoctorAiState(resolvedContextKey, (current) => ({
          ...current,
          activeSession: null,
          error: userMessage('AI sessions could not be loaded. Please try again.'),
          isLoadingSessions: false,
          loadedFor: null,
          sessions: [],
        }))
      }
    }

    loadSessions()
  }, [appointmentId, loadAttempt, patientId, resolvedContextKey, token])

  const renderedMessages = useMemo<RenderedAIMessage[]>(() => {
    const messages = activeSession?.messages ?? []

    if (!streamingReply) {
      return messages
    }

    return [
      ...messages,
      {
        id: `${streamingReply.id}-user`,
        role: 'user',
        content: streamingReply.question,
        createdAt: new Date().toISOString(),
      } satisfies AIMessage,
      {
        id: `${streamingReply.id}-assistant`,
        role: 'assistant',
        content: streamingReply.failed
          ? 'AI assistant is unavailable. Please try again.'
          : streamingReply.answer,
        sources: streamingReply.sources,
        createdAt: new Date().toISOString(),
        failed: streamingReply.failed,
        retryQuestion: streamingReply.question,
      } satisfies RenderedAIMessage,
    ]
  }, [activeSession, streamingReply])

  async function loadSession(sessionId: string) {
    updateDoctorAiState(resolvedContextKey, (current) => ({
      ...current,
      error: '',
      streamingReply: null,
    }))

    try {
      const loadedSession = await getAiSession(sessionId, token)
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        activeSession: loadedSession,
      }))
    } catch {
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        error: userMessage('AI session could not be opened. Please try again.'),
      }))
    }
  }

  async function createSession() {
    updateDoctorAiState(resolvedContextKey, (current) => ({
      ...current,
      error: '',
    }))

    const session = await createAiSession(
      {
        ...(patientId ? { patientId } : {}),
        ...(appointmentId ? { appointmentId } : {}),
        title: appointmentId ? `${title} appointment` : `${title} chat`,
      },
      token,
    )

    updateDoctorAiState(resolvedContextKey, (current) => ({
      ...current,
      activeSession: session,
      sessions: [session, ...current.sessions],
    }))
    return session
  }

  async function handleNewSession() {
    try {
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        streamingReply: null,
      }))
      await createSession()
    } catch {
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        error: userMessage('AI session could not be created. Please try again.'),
      }))
    }
  }

  async function handleDeleteSession() {
    const currentState = getDoctorAiState(resolvedContextKey)
    if (!currentState.activeSession || currentState.isStreaming) {
      return
    }

    updateDoctorAiState(resolvedContextKey, (current) => ({
      ...current,
      error: '',
    }))

    try {
      await deleteAiSession(currentState.activeSession.id, token)
      const remainingSessions = currentState.sessions.filter(
        (session) => session.id !== currentState.activeSession?.id,
      )
      let nextActiveSession: AISession | null = null

      if (remainingSessions[0]) {
        nextActiveSession = await getAiSession(remainingSessions[0].id, token)
      }

      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        activeSession: nextActiveSession,
        sessions: remainingSessions,
      }))
    } catch {
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        error: userMessage('AI session could not be deleted. Please try again.'),
      }))
    }
  }

  async function askAi(nextQuery: string, retryReplyId?: string) {
    const trimmed = nextQuery.trim()
    const currentState = getDoctorAiState(resolvedContextKey)

    if (!trimmed || currentState.isStreaming) {
      return
    }

    updateDoctorAiState(resolvedContextKey, (current) => ({
      ...current,
      error: '',
      isStreaming: true,
      streamingReply: {
        id: retryReplyId ?? `streaming-${Date.now()}`,
        question: trimmed,
        answer: '',
        sources: [],
        failed: false,
      },
    }))

    try {
      const session = currentState.activeSession ?? (await createSession())

      await streamAiMessage(session.id, trimmed, token, {
        onSources: (sources) => {
          updateDoctorAiState(resolvedContextKey, (current) => ({
            ...current,
            streamingReply: current.streamingReply
              ? { ...current.streamingReply, sources }
              : current.streamingReply,
          }))
        },
        onToken: (nextToken) => {
          updateDoctorAiState(resolvedContextKey, (current) => ({
            ...current,
            streamingReply: current.streamingReply
              ? {
                  ...current.streamingReply,
                  answer: `${current.streamingReply.answer}${nextToken}`,
                }
              : current.streamingReply,
          }))
        },
      })

      const refreshedSession = await getAiSession(session.id, token)
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        activeSession: refreshedSession,
        isStreaming: false,
        query: '',
        sessions: [refreshedSession, ...current.sessions.filter((item) => item.id !== session.id)].sort(
          (first, second) =>
            new Date(second.updatedAt).getTime() -
            new Date(first.updatedAt).getTime(),
        ),
        streamingReply: null,
      }))
    } catch {
      updateDoctorAiState(resolvedContextKey, (current) => ({
        ...current,
        isStreaming: false,
        streamingReply: current.streamingReply
          ? {
              ...current.streamingReply,
              answer: '',
              failed: true,
              sources: [],
            }
          : current.streamingReply,
      }))
    }
  }

  function retryMessage(message: RenderedAIMessage) {
    const retryId = message.id.endsWith('-assistant')
      ? message.id.slice(0, -'-assistant'.length)
      : undefined
    askAi(message.retryQuestion ?? '', retryId)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    askAi(query)
  }

  return (
    <section className="doctor-ai-chat">
      {error && (
        <StatusPanel title="AI assistant unavailable" text={error}>
          {!isLoadingSessions && loadedFor === null ? (
            <button
              className="secondary-button compact-button"
              onClick={() => setLoadAttempt((current) => current + 1)}
              type="button"
            >
              Retry
            </button>
          ) : null}
        </StatusPanel>
      )}

      <div className="ai-workbench">
        <aside className="ai-session-rail" aria-label="AI chat sessions">
          <div className="ai-rail-head">
            <span>Chats</span>
            <small>{sessions.length} saved</small>
          </div>
          <div className="ai-session-actions">
            <button
              className="secondary-button compact-button"
              disabled={isLoadingSessions || isStreaming}
              onClick={handleNewSession}
              type="button"
            >
              New chat
            </button>
            <button
              className="link-button ai-delete-button"
              disabled={!activeSession || isStreaming}
              onClick={handleDeleteSession}
              type="button"
            >
              Delete
            </button>
          </div>

          {isLoadingSessions ? (
            <StatusPanel title="Loading AI chats" />
          ) : sessions.length ? (
            <div className="ai-session-list">
              {sessions.map((session) => (
                <button
                  className={
                    session.id === activeSession?.id
                      ? 'ai-session-card active'
                      : 'ai-session-card'
                  }
                  disabled={isStreaming}
                  key={session.id}
                  onClick={() => loadSession(session.id)}
                  type="button"
                >
                  <strong>{session.title ?? 'Clinical chat'}</strong>
                  <span>
                    {session.appointmentId
                      ? 'Appointment'
                      : session.patientId
                        ? 'Patient'
                        : 'General'} ·{' '}
                    {formatAppointmentDate(session.updatedAt)}
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <EmptyPanel
              text={
                patientId
                  ? 'No saved AI chats for this patient yet.'
                  : 'No saved AI chats for this context yet.'
              }
            />
          )}
        </aside>

        <div className="ai-main-panel">
          <div className="ai-main-head">
            <div>
              <span>Conversation</span>
              <small>{renderedMessages.length} messages</small>
            </div>
            {prompts.length ? (
              <div className="ai-prompt-strip" aria-label="Suggested prompts">
                {prompts.map((prompt) => (
                  <button
                    disabled={isStreaming}
                    key={prompt}
                    onClick={() => askAi(prompt)}
                    type="button"
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            ) : null}
          </div>

          <div className="ai-thread" aria-live="polite">
            {renderedMessages.length ? (
              renderedMessages.map((message) => (
                <article
                  className={`ai-message ai-message-${message.role}${
                    message.failed ? ' ai-message-failed' : ''
                  }`}
                  key={message.id}
                >
                  <p>{message.content || (isStreaming ? 'Thinking...' : '')}</p>
                  {message.failed ? (
                    <button
                      className="secondary-button compact-button ai-retry-button"
                      disabled={isStreaming}
                      onClick={() => retryMessage(message)}
                      type="button"
                    >
                      Retry
                    </button>
                  ) : null}
                  {message.sources?.length ? (
                    <div className="ai-source-chips" aria-label="AI sources">
                      {message.sources.map((source, index) => (
                        <span key={`${source}-${index}`}>{source}</span>
                      ))}
                    </div>
                  ) : null}
                  {message.id.endsWith('-assistant') && isStreaming && (
                    <span className="ai-cursor" aria-hidden="true" />
                  )}
                </article>
              ))
            ) : (
              <div className="ai-empty">
                No messages in this chat.
              </div>
            )}
          </div>

          <form className="ai-compose" onSubmit={handleSubmit}>
            <label className="ai-compose-field">
              <span>{inputLabel}</span>
              <textarea
                onChange={(event) =>
                  updateDoctorAiState(resolvedContextKey, (current) => ({
                    ...current,
                    query: event.target.value,
                  }))
                }
                placeholder={placeholder}
                required
                rows={4}
                value={query}
              />
            </label>
            <button className="primary-button" disabled={isStreaming} type="submit">
              {isStreaming ? 'Streaming answer' : 'Ask AI'}
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}
