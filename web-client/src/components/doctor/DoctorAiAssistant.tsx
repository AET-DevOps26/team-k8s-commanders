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
  patientId: string
  appointmentId: string | null
  patientName: string
  token: string
}

type StreamingReply = {
  id: string
  question: string
  answer: string
  sources: string[]
}

const quickPrompts = [
  'Summarize recent visits and open risks.',
  'What changed since the last appointment?',
  'Draft questions for the next consultation.',
]

export function DoctorAiAssistant({
  patientId,
  appointmentId,
  patientName,
  token,
}: DoctorAiAssistantProps) {
  const [query, setQuery] = useState('')
  const [sessions, setSessions] = useState<AISessionSummary[]>([])
  const [activeSession, setActiveSession] = useState<AISession | null>(null)
  const [streamingReply, setStreamingReply] = useState<StreamingReply | null>(null)
  const [error, setError] = useState('')
  const [isLoadingSessions, setLoadingSessions] = useState(true)
  const [isStreaming, setStreaming] = useState(false)

  useEffect(() => {
    let isActive = true

    async function loadSessions() {
      setLoadingSessions(true)
      setError('')
      setActiveSession(null)
      setStreamingReply(null)

      try {
        const response = await listAiSessions(token, 0, 100)
        const patientSessions = response.content.filter(
          (session) => session.patientId === patientId,
        )

        if (!isActive) {
          return
        }

        setSessions(patientSessions)

        const preferredSession =
          patientSessions.find(
            (session) => appointmentId && session.appointmentId === appointmentId,
          ) ??
          patientSessions.find((session) => !session.appointmentId) ??
          patientSessions[0] ??
          null

        if (preferredSession) {
          const loadedSession = await getAiSession(preferredSession.id, token)
          if (isActive) {
            setActiveSession(loadedSession)
          }
        }
      } catch {
        if (isActive) {
          setError(userMessage('AI sessions could not be loaded. Please try again.'))
          setSessions([])
          setActiveSession(null)
        }
      } finally {
        if (isActive) {
          setLoadingSessions(false)
        }
      }
    }

    loadSessions()

    return () => {
      isActive = false
    }
  }, [appointmentId, patientId, token])

  const renderedMessages = useMemo(() => {
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
        content: streamingReply.answer,
        sources: streamingReply.sources,
        createdAt: new Date().toISOString(),
      } satisfies AIMessage,
    ]
  }, [activeSession, streamingReply])

  const sessionContextLabel = activeSession?.appointmentId
    ? 'Appointment context'
    : 'Patient context'

  async function loadSession(sessionId: string) {
    setError('')
    setStreamingReply(null)

    try {
      setActiveSession(await getAiSession(sessionId, token))
    } catch {
      setError(userMessage('AI session could not be opened. Please try again.'))
    }
  }

  async function createSession() {
    setError('')
    setStreamingReply(null)

    const session = await createAiSession(
      {
        patientId,
        ...(appointmentId ? { appointmentId } : {}),
        title: appointmentId ? `${patientName} appointment` : `${patientName} record`,
      },
      token,
    )

    setSessions((current) => [session, ...current])
    setActiveSession(session)
    return session
  }

  async function handleNewSession() {
    try {
      await createSession()
    } catch {
      setError(userMessage('AI session could not be created. Please try again.'))
    }
  }

  async function handleDeleteSession() {
    if (!activeSession || isStreaming) {
      return
    }

    setError('')

    try {
      await deleteAiSession(activeSession.id, token)
      const remainingSessions = sessions.filter(
        (session) => session.id !== activeSession.id,
      )
      setSessions(remainingSessions)
      setActiveSession(null)

      if (remainingSessions[0]) {
        setActiveSession(await getAiSession(remainingSessions[0].id, token))
      }
    } catch {
      setError(userMessage('AI session could not be deleted. Please try again.'))
    }
  }

  async function askAi(nextQuery: string) {
    const trimmed = nextQuery.trim()

    if (!trimmed || isStreaming) {
      return
    }

    setError('')
    setStreaming(true)
    setStreamingReply({
      id: `streaming-${Date.now()}`,
      question: trimmed,
      answer: '',
      sources: [],
    })

    try {
      const session = activeSession ?? (await createSession())

      await streamAiMessage(session.id, trimmed, token, {
        onSources: (sources) => {
          setStreamingReply((current) =>
            current ? { ...current, sources } : current,
          )
        },
        onToken: (nextToken) => {
          setStreamingReply((current) =>
            current
              ? { ...current, answer: `${current.answer}${nextToken}` }
              : current,
          )
        },
      })

      const refreshedSession = await getAiSession(session.id, token)
      setActiveSession(refreshedSession)
      setSessions((current) =>
        [refreshedSession, ...current.filter((item) => item.id !== session.id)].sort(
          (first, second) =>
            new Date(second.updatedAt).getTime() -
            new Date(first.updatedAt).getTime(),
        ),
      )
      setQuery('')
      setStreamingReply(null)
    } catch {
      setError(userMessage('AI assistant is unavailable. Please try again.'))
    } finally {
      setStreaming(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    askAi(query)
  }

  return (
    <section className="doctor-ai-chat">
      <div className="doctor-ai-head">
        <div>
          <p className="eyebrow">AI assistant</p>
          <h3>{patientName}</h3>
        </div>
        <span className={isStreaming ? 'ai-live-badge is-live' : 'ai-live-badge'}>
          {isStreaming ? 'Streaming' : sessionContextLabel}
        </span>
      </div>

      {error && <StatusPanel title="AI assistant unavailable" text={error} />}

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
                    {session.appointmentId ? 'Appointment' : 'Patient'} ·{' '}
                    {formatAppointmentDate(session.updatedAt)}
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <EmptyPanel text="No saved AI chats for this patient yet." />
          )}
        </aside>

        <div className="ai-main-panel">
          <div className="ai-main-head">
            <div>
              <span>Conversation</span>
              <small>{renderedMessages.length} messages</small>
            </div>
            <div className="ai-prompt-strip" aria-label="Suggested prompts">
              {quickPrompts.map((prompt) => (
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
          </div>

          <div className="ai-thread" aria-live="polite">
            {renderedMessages.length ? (
              renderedMessages.map((message) => (
                <article
                  className={`ai-message ai-message-${message.role}`}
                  key={message.id}
                >
                  <p>{message.content || (isStreaming ? 'Thinking...' : '')}</p>
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
              <span>Ask about this patient</span>
              <textarea
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Summarize the last three visits and flag anything I should review."
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
