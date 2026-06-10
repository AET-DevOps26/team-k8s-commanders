import { useState } from 'react'
import type { Appointment, ScheduleSlot } from '../../clientApi'
import {
  cancelAppointment,
  getDoctorSchedule,
  rescheduleAppointment,
} from '../../clientApi'
import {
  formatAppointmentDate,
  formatTimeRange,
  isPastDateTime,
  slotDuration,
} from '../../lib/dates'
import { EmptyPanel } from '../ui/EmptyPanel'

type AppointmentRowProps = {
  appointment: Appointment
  onChanged: () => void
  token: string
}

export function AppointmentRow({ appointment, onChanged, token }: AppointmentRowProps) {
  const [availableSlots, setAvailableSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [isMoving, setMoving] = useState(false)
  const [message, setMessage] = useState('')
  const [isBusy, setBusy] = useState(false)
  const isPast = isPastDateTime(appointment.dateTime)
  const canChange =
    appointment.status !== 'CANCELLED' &&
    appointment.status !== 'COMPLETED' &&
    !isPast

  async function handleCancel() {
    if (!canChange) {
      return
    }

    setBusy(true)
    setMessage('')

    try {
      await cancelAppointment(token, appointment.id)
      setMessage('Appointment cancelled')
      onChanged()
    } catch {
      setMessage('Appointment could not be cancelled. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  async function openMoveOptions() {
    if (!canChange) {
      return
    }

    setMoving((current) => !current)
    setMessage('')

    if (availableSlots.length) {
      return
    }

    setBusy(true)

    try {
      const schedule = await getDoctorSchedule(appointment.doctorId, token)
      setAvailableSlots(schedule.slots)
    } catch {
      setMessage('Available times could not be loaded. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  async function handleReschedule() {
    if (!canChange) {
      return
    }

    if (!selectedSlot) {
      setMessage('Please choose a new time.')
      return
    }

    setBusy(true)
    setMessage('')

    try {
      await rescheduleAppointment(token, appointment.id, {
        dateTime: selectedSlot.startAt,
        duration: slotDuration(selectedSlot),
      })
      setMessage('Appointment moved')
      setMoving(false)
      setSelectedSlot(null)
      onChanged()
    } catch {
      setMessage('Appointment could not be moved. Please choose another time.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className={isPast ? 'appointment-item appointment-item-past' : 'appointment-item'}>
      <div>
        <strong>{formatAppointmentDate(appointment.dateTime)}</strong>
        <p>{appointment.reason ?? 'No reason provided'}</p>
        {message && <small>{message}</small>}
      </div>
      <div className="appointment-actions">
        {isPast && <span className="appointment-status-past">PAST</span>}
        <span>{appointment.status}</span>
        {canChange && (
          <>
            <button disabled={isBusy} onClick={openMoveOptions} type="button">
              Move
            </button>
            <button disabled={isBusy} onClick={handleCancel} type="button">
              Cancel
            </button>
          </>
        )}
      </div>
      {isMoving && (
        <div className="move-panel">
          {availableSlots.length ? (
            <>
              <div className="move-slot-grid">
                {availableSlots.map((slot) => (
                  <button
                    className={selectedSlot?.startAt === slot.startAt ? 'active' : ''}
                    key={`${appointment.id}-${slot.startAt}`}
                    onClick={() => setSelectedSlot(slot)}
                    type="button"
                  >
                    <strong>{formatAppointmentDate(slot.startAt)}</strong>
                    <span>{formatTimeRange(slot)}</span>
                  </button>
                ))}
              </div>
              <button
                className="primary-button"
                disabled={isBusy}
                onClick={handleReschedule}
                type="button"
              >
                Confirm new time
              </button>
            </>
          ) : (
            <EmptyPanel text="No other times available right now." />
          )}
        </div>
      )}
    </div>
  )
}
