import { useState } from 'react'
import type { Appointment, ScheduleSlot } from '../../clientApi'
import {
  cancelAppointment,
  getDoctorSchedule,
  rescheduleAppointment,
} from '../../clientApi'
import { formatAppointmentDate, isPastDateTime, slotDuration } from '../../lib/dates'
import { SlotPicker } from '../booking/SlotPicker'
import { EmptyPanel } from '../ui/EmptyPanel'

type AppointmentRowProps = {
  appointment: Appointment
  onChanged: () => void
  token: string
  isMoving: boolean
  onMoveOpen: () => void
  onMoveClose: () => void
}

export function AppointmentRow({
  appointment,
  onChanged,
  token,
  isMoving,
  onMoveOpen,
  onMoveClose,
}: AppointmentRowProps) {
  const [availableSlots, setAvailableSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [message, setMessage] = useState('')
  const [isBusy, setBusy] = useState(false)
  const isPast = isPastDateTime(appointment.dateTime)
  const canChange =
    appointment.status !== 'CANCELLED' &&
    appointment.status !== 'COMPLETED' &&
    !isPast
  const isLoadingSlots = isBusy && !availableSlots.length

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

    onMoveOpen()
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

  function closeMove() {
    setSelectedSlot(null)
    setMessage('')
    onMoveClose()
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
        {isMoving && <small>Pick a new time for this appointment.</small>}
        {message && <small>{message}</small>}
      </div>
      <div className="appointment-actions">
        {isPast && <span className="appointment-status-past">PAST</span>}
        <span>{appointment.status}</span>
        {canChange && !isMoving && (
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
          <div className="move-panel-scroll">
            {isLoadingSlots ? (
              <EmptyPanel text="Loading available times…" />
            ) : (
              <SlotPicker
                slots={availableSlots}
                selectedSlot={selectedSlot}
                onSelectSlot={setSelectedSlot}
                emptyText="No other times available right now."
              />
            )}
          </div>
          <div className="move-panel-actions">
            <button
              className="secondary-button"
              disabled={isBusy}
              onClick={closeMove}
              type="button"
            >
              Cancel reschedule
            </button>
            {availableSlots.length > 0 && (
              <button
                className="primary-button"
                disabled={isBusy || !selectedSlot}
                onClick={handleReschedule}
                type="button"
              >
                Confirm new time
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
