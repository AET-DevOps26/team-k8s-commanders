import { useState } from 'react'
import type { ScheduleSlot } from '../../clientApi'
import { formatTimeRange, isPastDateTime } from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'

type AvailabilitySlotsPanelProps = {
  slots: ScheduleSlot[]
  onDelete: (slotId: string) => Promise<void>
}

export function AvailabilitySlotsPanel({
  slots,
  onDelete,
}: AvailabilitySlotsPanelProps) {
  const [deletingId, setDeletingId] = useState('')

  const futureSlots = slots
    .filter((slot) => !isPastDateTime(slot.startAt))
    .sort(
      (left, right) =>
        new Date(left.startAt).getTime() - new Date(right.startAt).getTime(),
    )

  async function handleDelete(slot: ScheduleSlot) {
    if (!window.confirm('Remove this slot? Patients will no longer be able to book it.')) {
      return
    }

    setDeletingId(slot.id)
    try {
      await onDelete(slot.id)
    } finally {
      setDeletingId('')
    }
  }

  return (
    <section className="dashboard-panel availability-slots-panel">
      <div className="panel-header doctor-panel-header">
        <div>
          <p className="eyebrow">Published</p>
          <h2>Slots</h2>
        </div>
        <span className="availability-count">{futureSlots.length}</span>
      </div>

      {futureSlots.length ? (
        <div className="availability-slot-list">
          {futureSlots.map((slot) => (
            <article
              className={
                slot.available
                  ? 'availability-slot-card'
                  : 'availability-slot-card availability-slot-card-booked'
              }
              key={slot.id}
            >
              <div className="availability-slot-date">
                <span>{formatSlotWeekday(slot.startAt)}</span>
                <strong>{formatSlotDate(slot.startAt)}</strong>
              </div>
              <div className="availability-slot-meta">
                <span className="availability-time-range">{formatTimeRange(slot)}</span>
                {!slot.available && <small>Booked</small>}
              </div>
              {slot.available && (
                <button
                  className="availability-slot-delete"
                  disabled={deletingId === slot.id}
                  onClick={() => handleDelete(slot)}
                  type="button"
                >
                  {deletingId === slot.id ? 'Removing…' : 'Remove'}
                </button>
              )}
            </article>
          ))}
        </div>
      ) : (
        <EmptyPanel text="No future slots published." />
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
