import { useEffect, useMemo, useState } from 'react'
import type { ScheduleSlot } from '../../clientApi'
import { formatAppointmentDate, formatTimeRange, isPastDateTime } from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'

type BookingCalendarSectionProps = {
  subjectName: string
  slots: ScheduleSlot[]
  selectedSlot: ScheduleSlot | null
  onSelectSlot: (slot: ScheduleSlot) => void
  reason: string
  onReasonChange: (value: string) => void
  onCancel: () => void
  onBook: () => void
  cancelLabel: string
  bookLabel: string
}

type SlotDayGroup = {
  key: string
  label: string
  shortLabel: string
  slots: ScheduleSlot[]
}

const DAYS_PER_PAGE = 7

const dayLabelFormatter = new Intl.DateTimeFormat('de-DE', {
  weekday: 'long',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

const shortDayLabelFormatter = new Intl.DateTimeFormat('de-DE', {
  weekday: 'short',
  day: '2-digit',
  month: '2-digit',
})

function slotDayKey(slot: ScheduleSlot) {
  const date = new Date(slot.startAt)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function groupSlotsByDay(slots: ScheduleSlot[]) {
  const groups = new Map<string, SlotDayGroup>()

  slots.forEach((slot) => {
    const key = slotDayKey(slot)
    const date = new Date(slot.startAt)
    const existing = groups.get(key)

    if (existing) {
      existing.slots.push(slot)
      return
    }

    groups.set(key, {
      key,
      label: dayLabelFormatter.format(date),
      shortLabel: shortDayLabelFormatter.format(date),
      slots: [slot],
    })
  })

  return [...groups.values()].map((group) => ({
    ...group,
    slots: group.slots.sort(
      (first, second) =>
        new Date(first.startAt).getTime() - new Date(second.startAt).getTime(),
    ),
  }))
}

export function BookingCalendarSection({
  subjectName,
  slots,
  selectedSlot,
  onSelectSlot,
  reason,
  onReasonChange,
  onCancel,
  onBook,
  cancelLabel,
  bookLabel,
}: BookingCalendarSectionProps) {
  const now = Date.now()
  const futureSlots = useMemo(
    () =>
      slots
        .filter((slot) => !isPastDateTime(slot.startAt, now))
        .sort(
          (first, second) =>
            new Date(first.startAt).getTime() - new Date(second.startAt).getTime(),
        ),
    [now, slots],
  )
  const dayGroups = useMemo(() => groupSlotsByDay(futureSlots), [futureSlots])
  const selectedDayKey = selectedSlot ? slotDayKey(selectedSlot) : null
  const [activeDayKey, setActiveDayKey] = useState<string | null>(
    selectedDayKey ?? dayGroups[0]?.key ?? null,
  )
  const [dayPage, setDayPage] = useState(0)
  const activeDay = dayGroups.find((group) => group.key === activeDayKey) ?? dayGroups[0]
  const activeDayIndex = activeDay
    ? dayGroups.findIndex((group) => group.key === activeDay.key)
    : 0
  const pageCount = Math.max(1, Math.ceil(dayGroups.length / DAYS_PER_PAGE))
  const currentPage =
    pageCount > 1 && activeDayIndex >= 0
      ? Math.floor(activeDayIndex / DAYS_PER_PAGE)
      : dayPage
  const boundedPage = Math.min(Math.max(currentPage, 0), pageCount - 1)
  const visibleDayGroups = dayGroups.slice(
    boundedPage * DAYS_PER_PAGE,
    boundedPage * DAYS_PER_PAGE + DAYS_PER_PAGE,
  )
  const visibleDayStart = boundedPage * DAYS_PER_PAGE + 1
  const visibleDayEnd = Math.min((boundedPage + 1) * DAYS_PER_PAGE, dayGroups.length)

  useEffect(() => {
    if (selectedDayKey && dayGroups.some((group) => group.key === selectedDayKey)) {
      setActiveDayKey(selectedDayKey)
      return
    }

    if (!dayGroups.some((group) => group.key === activeDayKey)) {
      setActiveDayKey(dayGroups[0]?.key ?? null)
    }
  }, [activeDayKey, dayGroups, selectedDayKey])

  useEffect(() => {
    if (boundedPage !== dayPage) {
      setDayPage(boundedPage)
    }
  }, [boundedPage, dayPage])

  function moveDayPage(direction: -1 | 1) {
    const nextPage = Math.min(Math.max(boundedPage + direction, 0), pageCount - 1)
    const firstDayOnPage = dayGroups[nextPage * DAYS_PER_PAGE]

    setDayPage(nextPage)
    if (firstDayOnPage) {
      setActiveDayKey(firstDayOnPage.key)
    }
  }

  return (
    <section className="calendar-panel dashboard-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Calendar</p>
          <h2>{subjectName}</h2>
        </div>
      </div>
      {futureSlots.length ? (
        <div className="slot-picker">
          <div className="slot-week-controls" aria-label="Available day range">
            <button
              aria-label="Show previous week"
              className="slot-week-button"
              disabled={boundedPage === 0}
              onClick={() => moveDayPage(-1)}
              type="button"
            >
              ‹
            </button>
            <span>
              Days {visibleDayStart}-{visibleDayEnd} of {dayGroups.length}
            </span>
            <button
              aria-label="Show next week"
              className="slot-week-button"
              disabled={boundedPage >= pageCount - 1}
              onClick={() => moveDayPage(1)}
              type="button"
            >
              ›
            </button>
          </div>

          <div className="slot-day-list" aria-label="Available days" role="list">
            {visibleDayGroups.map((group) => (
              <button
                aria-pressed={group.key === activeDay?.key}
                className={
                  group.key === activeDay?.key
                    ? 'slot-day-button active'
                    : 'slot-day-button'
                }
                key={group.key}
                onClick={() => {
                  setActiveDayKey(group.key)
                  setDayPage(
                    Math.floor(
                      dayGroups.findIndex((dayGroup) => dayGroup.key === group.key) /
                        DAYS_PER_PAGE,
                    ),
                  )
                }}
                type="button"
              >
                <span>{group.shortLabel}</span>
                <strong>{group.slots.length} slots</strong>
              </button>
            ))}
          </div>

          <div className="slot-picker-header">
            <span>{activeDay?.label}</span>
            <small>{activeDay?.slots.length ?? 0} available times</small>
          </div>

          <div className="slot-grid slot-time-grid">
            {activeDay?.slots.map((slot) => (
              <button
                className={
                  selectedSlot?.startAt === slot.startAt
                    ? 'slot-button active'
                    : 'slot-button'
                }
                key={`${slot.startAt}-${slot.endAt}`}
                onClick={() => onSelectSlot(slot)}
                type="button"
              >
                <span>{formatAppointmentDate(slot.startAt)}</span>
                <strong>{formatTimeRange(slot)}</strong>
              </button>
            ))}
          </div>
        </div>
      ) : (
        <EmptyPanel text="No available times right now." />
      )}
      <label className="reason-field">
        Reason
        <textarea
          onChange={(event) => onReasonChange(event.target.value)}
          placeholder="Short reason for the visit"
          rows={3}
          value={reason}
        />
      </label>
      <div className="quick-actions">
        <button className="secondary-button" onClick={onCancel} type="button">
          {cancelLabel}
        </button>
        <button className="primary-button" onClick={onBook} type="button">
          {bookLabel}
        </button>
      </div>
    </section>
  )
}
