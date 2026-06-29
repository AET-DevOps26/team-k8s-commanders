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
  const futureSlots = slots.filter((slot) => !isPastDateTime(slot.startAt, now))

  return (
    <section className="calendar-panel dashboard-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Calendar</p>
          <h2>{subjectName}</h2>
        </div>
      </div>
      <div className="slot-grid">
        {futureSlots.length ? (
          futureSlots.map((slot) => (
            <button
              className={selectedSlot?.startAt === slot.startAt ? 'slot-button active' : 'slot-button'}
              key={`${slot.startAt}-${slot.endAt}`}
              onClick={() => onSelectSlot(slot)}
              type="button"
            >
              <span>{formatAppointmentDate(slot.startAt)}</span>
              <strong>{formatTimeRange(slot)}</strong>
            </button>
          ))
        ) : (
          <EmptyPanel text="No available times right now." />
        )}
      </div>
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
