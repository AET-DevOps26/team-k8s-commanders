import type { ScheduleSlot } from '../../clientApi'
import { SlotPicker } from './SlotPicker'

type BookingCalendarSectionProps = {
  subjectName: string
  slots: ScheduleSlot[]
  selectedSlot: ScheduleSlot | null
  onSelectSlot: (slot: ScheduleSlot | null) => void
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
  return (
    <section className="calendar-panel dashboard-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Calendar</p>
          <h2>{subjectName}</h2>
        </div>
      </div>
      <SlotPicker
        slots={slots}
        selectedSlot={selectedSlot}
        onSelectSlot={onSelectSlot}
      />
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
