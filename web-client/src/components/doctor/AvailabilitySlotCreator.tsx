import { useMemo, useState, type FormEvent } from 'react'
import type { ScheduleSlot } from '../../clientApi'
import { createDoctorScheduleSlot } from '../../clientApi'
import { userMessage } from '../../lib/messages'
import { StatusPanel } from '../ui/StatusPanel'

type AvailabilitySlotCreatorProps = {
  doctorId: string
  token: string
  onCreated: (slot: ScheduleSlot) => void
}

const DURATIONS = [15, 30, 45, 60]

export function AvailabilitySlotCreator({
  doctorId,
  token,
  onCreated,
}: AvailabilitySlotCreatorProps) {
  const [startAt, setStartAt] = useState('')
  const [duration, setDuration] = useState(30)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const minStartAt = useMemo(() => toDatetimeLocalValue(new Date()), [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setStatus('')
    setError('')

    const startDate = new Date(startAt)
    if (!startAt || Number.isNaN(startDate.getTime())) {
      setError('Choose a valid start time.')
      return
    }

    if (startDate.getTime() <= Date.now()) {
      setError('Choose a future time.')
      return
    }

    const endDate = new Date(startDate.getTime() + duration * 60000)
    setSubmitting(true)

    try {
      const created = await createDoctorScheduleSlot(doctorId, token, {
        startAt: startDate.toISOString(),
        endAt: endDate.toISOString(),
      })
      setStartAt('')
      setStatus('Availability published.')
      onCreated(created)
    } catch {
      setError(userMessage('Availability could not be published. Check the time range.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="dashboard-panel availability-creator" onSubmit={handleSubmit}>
      <div className="panel-header doctor-panel-header">
        <div>
          <p className="eyebrow">New availability</p>
          <h2>Publish slot</h2>
        </div>
      </div>

      {error && <StatusPanel title="Slot unavailable" text={error} />}
      {status && <p className="form-status">{status}</p>}

      <div className="availability-form-grid">
        <label>
          Start
          <input
            min={minStartAt}
            onChange={(event) => setStartAt(event.target.value)}
            required
            type="datetime-local"
            value={startAt}
          />
        </label>

        <label>
          Duration
          <select
            onChange={(event) => setDuration(Number(event.target.value))}
            value={duration}
          >
            {DURATIONS.map((minutes) => (
              <option key={minutes} value={minutes}>
                {minutes} minutes
              </option>
            ))}
          </select>
        </label>
      </div>

      <button className="primary-button" disabled={isSubmitting} type="submit">
        {isSubmitting ? 'Publishing' : 'Publish availability'}
      </button>
    </form>
  )
}

function toDatetimeLocalValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return offsetDate.toISOString().slice(0, 16)
}
