import { useMemo, useState, type FormEvent } from 'react'
import type {
  RecurringScheduleResult,
  ScheduleSlot,
  Weekday,
} from '../../clientApi'
import {
  createDoctorRecurringSchedule,
  createDoctorScheduleSlot,
} from '../../clientApi'
import { userMessage } from '../../lib/messages'
import { StatusPanel } from '../ui/StatusPanel'

type AvailabilitySlotCreatorProps = {
  doctorId: string
  token: string
  onCreated: (slot: ScheduleSlot) => void
  onCreatedMany: (slots: ScheduleSlot[]) => void
}

const DURATIONS = [15, 30, 45, 60]

const WEEKDAYS: { value: Weekday; label: string }[] = [
  { value: 'MONDAY', label: 'Mon' },
  { value: 'TUESDAY', label: 'Tue' },
  { value: 'WEDNESDAY', label: 'Wed' },
  { value: 'THURSDAY', label: 'Thu' },
  { value: 'FRIDAY', label: 'Fri' },
  { value: 'SATURDAY', label: 'Sat' },
  { value: 'SUNDAY', label: 'Sun' },
]

const MAX_HORIZON_DAYS = 84

export function AvailabilitySlotCreator({
  doctorId,
  token,
  onCreated,
  onCreatedMany,
}: AvailabilitySlotCreatorProps) {
  const [mode, setMode] = useState<'single' | 'recurring'>('single')
  const [startAt, setStartAt] = useState('')
  const [duration, setDuration] = useState(30)
  const [weekdays, setWeekdays] = useState<Weekday[]>([])
  const [windowStart, setWindowStart] = useState('09:00')
  const [windowEnd, setWindowEnd] = useState('12:00')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const minStartAt = useMemo(() => toDatetimeLocalValue(new Date()), [])
  const today = useMemo(() => toDateValue(new Date()), [])
  const maxEndDate = useMemo(() => {
    if (!startDate) {
      return undefined
    }
    const limit = new Date(`${startDate}T00:00:00`)
    limit.setDate(limit.getDate() + MAX_HORIZON_DAYS)
    return toDateValue(limit)
  }, [startDate])
  const timezone = useMemo(
    () => Intl.DateTimeFormat().resolvedOptions().timeZone,
    [],
  )

  const preview = useMemo(
    () =>
      previewRecurring(weekdays, windowStart, windowEnd, duration, startDate, endDate),
    [weekdays, windowStart, windowEnd, duration, startDate, endDate],
  )

  function toggleWeekday(day: Weekday) {
    setWeekdays((current) =>
      current.includes(day)
        ? current.filter((entry) => entry !== day)
        : [...current, day],
    )
  }

  function switchMode(next: 'single' | 'recurring') {
    setMode(next)
    setStatus('')
    setError('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setStatus('')
    setError('')

    if (mode === 'single') {
      await submitSingle()
      return
    }
    await submitRecurring()
  }

  async function submitSingle() {
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

  async function submitRecurring() {
    if (!weekdays.length) {
      setError('Pick at least one weekday.')
      return
    }
    if (!windowStart || !windowEnd || windowEnd <= windowStart) {
      setError('The end time must be after the start time.')
      return
    }
    if (minutesBetween(windowStart, windowEnd) < duration) {
      setError('The time window is shorter than one slot.')
      return
    }
    if (!startDate || !endDate) {
      setError('Choose a first and last day.')
      return
    }
    if (endDate < startDate) {
      setError('The last day must not be before the first day.')
      return
    }
    if (maxEndDate && endDate > maxEndDate) {
      setError('Recurring availability may cover at most 12 weeks.')
      return
    }

    setSubmitting(true)

    try {
      const result = await createDoctorRecurringSchedule(doctorId, token, {
        weekdays,
        startTime: windowStart,
        endTime: windowEnd,
        slotDurationMinutes: duration as 15 | 30 | 45 | 60,
        startDate,
        endDate,
        timezone,
      })
      setStatus(recurringStatus(result))
      onCreatedMany(result.created)
    } catch {
      setError(userMessage('Recurring availability could not be published.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="dashboard-panel availability-creator" onSubmit={handleSubmit}>
      <div className="panel-header doctor-panel-header">
        <div>
          <p className="eyebrow">New availability</p>
          <h2>Publish slots</h2>
        </div>
        <div className="segmented-control" aria-label="Availability mode">
          <button
            className={mode === 'single' ? 'active' : ''}
            onClick={() => switchMode('single')}
            type="button"
          >
            Single
          </button>
          <button
            className={mode === 'recurring' ? 'active' : ''}
            onClick={() => switchMode('recurring')}
            type="button"
          >
            Weekly
          </button>
        </div>
      </div>

      {error && <StatusPanel title="Slot unavailable" text={error} />}
      {status && <p className="form-status">{status}</p>}

      {mode === 'single' ? (
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
      ) : (
        <>
          <div className="weekday-picker" role="group" aria-label="Weekdays">
            {WEEKDAYS.map((day) => (
              <button
                className={
                  weekdays.includes(day.value)
                    ? 'weekday-chip weekday-chip-active'
                    : 'weekday-chip'
                }
                aria-pressed={weekdays.includes(day.value)}
                key={day.value}
                onClick={() => toggleWeekday(day.value)}
                type="button"
              >
                {day.label}
              </button>
            ))}
          </div>

          <div className="availability-form-grid">
            <label>
              From
              <input
                onChange={(event) => setWindowStart(event.target.value)}
                required
                step={900}
                type="time"
                value={windowStart}
              />
            </label>

            <label>
              Until
              <input
                onChange={(event) => setWindowEnd(event.target.value)}
                required
                step={900}
                type="time"
                value={windowEnd}
              />
            </label>

            <label>
              Slot length
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

            <label>
              First day
              <input
                min={today}
                onChange={(event) => setStartDate(event.target.value)}
                required
                type="date"
                value={startDate}
              />
            </label>

            <label>
              Last day
              <input
                max={maxEndDate}
                min={startDate || today}
                onChange={(event) => setEndDate(event.target.value)}
                required
                type="date"
                value={endDate}
              />
            </label>
          </div>

          {preview !== null && (
            <p className="recurring-preview">
              ≈ {preview.slots} slots across {preview.days} day
              {preview.days === 1 ? '' : 's'} · times in {timezone}
            </p>
          )}
        </>
      )}

      <button className="primary-button" disabled={isSubmitting} type="submit">
        {isSubmitting ? 'Publishing' : 'Publish availability'}
      </button>
    </form>
  )
}

function recurringStatus(result: RecurringScheduleResult) {
  const created = result.created.length
  const skipped = result.skipped.length
  const base =
    created === 1 ? 'Published 1 slot.' : `Published ${created} slots.`
  if (!skipped) {
    return base
  }
  return `${base} Skipped ${skipped} overlapping with existing slots.`
}

/**
 * Local mirror of the server-side expansion, ignoring conflicts: counts
 * matching weekdays in the range and full slots per daily window.
 */
function previewRecurring(
  weekdays: Weekday[],
  windowStart: string,
  windowEnd: string,
  duration: number,
  startDate: string,
  endDate: string,
) {
  if (!weekdays.length || !startDate || !endDate || endDate < startDate) {
    return null
  }
  const windowMinutes = minutesBetween(windowStart, windowEnd)
  if (windowMinutes < duration) {
    return null
  }
  const slotsPerDay = Math.floor(windowMinutes / duration)

  // JS getDay(): 0 = Sunday; WEEKDAYS list starts at Monday.
  const selectedDayIndexes = new Set(
    weekdays.map(
      (day) => (WEEKDAYS.findIndex((entry) => entry.value === day) + 1) % 7,
    ),
  )

  let days = 0
  const cursor = new Date(`${startDate}T00:00:00`)
  const last = new Date(`${endDate}T00:00:00`)
  for (let guard = 0; cursor <= last && guard <= MAX_HORIZON_DAYS; guard += 1) {
    if (selectedDayIndexes.has(cursor.getDay())) {
      days += 1
    }
    cursor.setDate(cursor.getDate() + 1)
  }

  return { days, slots: days * slotsPerDay }
}

function minutesBetween(start: string, end: string) {
  const [startHours, startMinutes] = start.split(':').map(Number)
  const [endHours, endMinutes] = end.split(':').map(Number)
  if ([startHours, startMinutes, endHours, endMinutes].some(Number.isNaN)) {
    return 0
  }
  return endHours * 60 + endMinutes - (startHours * 60 + startMinutes)
}

function toDatetimeLocalValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return offsetDate.toISOString().slice(0, 16)
}

function toDateValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return offsetDate.toISOString().slice(0, 10)
}
