import { useEffect, useState, type FormEvent } from 'react'
import type { ClinicalNoteInput } from '../../clientApi'
import { getAppointmentNote, queryAi, upsertAppointmentNote } from '../../clientApi'
import { userMessage } from '../../lib/messages'

type NoteEditorProps = {
  appointmentId: string
  patientId: string
  token: string
  onSaved?: (appointmentId: string) => void
}

export function NoteEditor({
  appointmentId,
  patientId,
  token,
  onSaved,
}: NoteEditorProps) {
  const [content, setContent] = useState('')
  const [diagnosisCode, setDiagnosisCode] = useState('')
  const [diagnosisDescription, setDiagnosisDescription] = useState('')
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const [isDrafting, setDrafting] = useState(false)

  useEffect(() => {
    let isActive = true

    async function loadNote() {
      setStatus('')
      setError('')

      try {
        const note = await getAppointmentNote(appointmentId, token)

        if (isActive && note) {
          setContent(note.content)
          setDiagnosisCode(note.diagnosis?.code ?? '')
          setDiagnosisDescription(note.diagnosis?.description ?? '')
        } else if (isActive) {
          setContent('')
          setDiagnosisCode('')
          setDiagnosisDescription('')
        }
      } catch {
        if (isActive) {
          setError(userMessage('Existing note could not be loaded. Please try again.'))
        }
      }
    }

    loadNote()

    return () => {
      isActive = false
    }
  }, [appointmentId, token])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setStatus('')
    setSubmitting(true)

    const input: ClinicalNoteInput = { content }
    if (diagnosisCode.trim() && diagnosisDescription.trim()) {
      input.diagnosis = {
        code: diagnosisCode.trim(),
        description: diagnosisDescription.trim(),
      }
    }

    try {
      await upsertAppointmentNote(appointmentId, input, token)
      setStatus('Clinical note saved.')
      onSaved?.(appointmentId)
    } catch {
      setError(userMessage('Clinical note could not be saved. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDraft() {
    setError('')
    setStatus('')
    setDrafting(true)

    try {
      const response = await queryAi(
        {
          patientId,
          appointmentId,
          query:
            'Draft a concise clinical note for this appointment, summarizing the relevant patient history.',
        },
        token,
      )
      setContent((current) =>
        current ? `${current}\n\n${response.answer}` : response.answer,
      )
      setStatus('AI draft inserted. Review before saving.')
    } catch {
      setError(userMessage('AI draft could not be generated. Please try again.'))
    } finally {
      setDrafting(false)
    }
  }

  return (
    <form className="note-form doctor-note-editor" onSubmit={handleSubmit}>
      <div className="note-form-head">
        <div>
          <p className="eyebrow">Clinical note</p>
          <h3>Appointment note</h3>
        </div>
        <button
          className="secondary-button compact-button"
          disabled={isDrafting}
          onClick={handleDraft}
          type="button"
        >
          {isDrafting ? 'Drafting' : 'Draft with AI'}
        </button>
      </div>

      <label>
        Note
        <textarea
          onChange={(event) => setContent(event.target.value)}
          required
          rows={7}
          value={content}
        />
      </label>

      <div className="note-form-row">
        <label>
          Diagnosis code
          <input
            onChange={(event) => setDiagnosisCode(event.target.value)}
            placeholder="e.g. J06.9"
            type="text"
            value={diagnosisCode}
          />
        </label>
        <label>
          Diagnosis description
          <input
            onChange={(event) => setDiagnosisDescription(event.target.value)}
            placeholder="e.g. Acute upper respiratory infection"
            type="text"
            value={diagnosisDescription}
          />
        </label>
      </div>

      {error && <p className="form-error">{error}</p>}
      {status && <p className="form-status">{status}</p>}

      <button className="primary-button" disabled={isSubmitting || isDrafting} type="submit">
        {isSubmitting ? 'Saving' : 'Save note'}
      </button>
    </form>
  )
}
