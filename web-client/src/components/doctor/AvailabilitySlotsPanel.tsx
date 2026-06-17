import type { ScheduleSlot } from '../../clientApi'
import { formatTimeRange, isPastDateTime } from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'

type AvailabilitySlotsPanelProps = {
  slots: ScheduleSlot[]
}

export function AvailabilitySlotsPanel({ slots }: AvailabilitySlotsPanelProps) {
  const futureSlots = slots
    .filter((slot) => slot.available && !isPastDateTime(slot.startAt))
    .sort(
      (left, right) =>
        new Date(left.startAt).getTime() - new Date(right.startAt).getTime(),
    )

  return (
    <section className="dashboard-panel availability-slots-panel">
      <div className="panel-header doctor-panel-header">
        <div>
          <p className="eyebrow">Published</p>
          <h2>Open slots</h2>
        </div>
        <span className="availability-count">{futureSlots.length}</span>
      </div>

      {futureSlots.length ? (
        <div className="availability-slot-list">
          {futureSlots.map((slot) => (
            <article className="availability-slot-card" key={`${slot.startAt}-${slot.endAt}`}>
              <div className="availability-slot-date">
                <span>{formatSlotWeekday(slot.startAt)}</span>
                <strong>{formatSlotDate(slot.startAt)}</strong>
              </div>
              <div className="availability-slot-meta">
                <span className="availability-time-range">{formatTimeRange(slot)}</span>
                <small>Available</small>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyPanel text="No future availability published." />
      )}
    </section>
  )
}

function formatSlotWeekday(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    weekday: 'short',
  }).format(new Date(value))
}

function formatSlotDate(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
  }).format(new Date(value))
}
