import type { ScheduleSlot } from '../clientApi'

export function formatAppointmentDate(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function formatTimeRange(slot: ScheduleSlot) {
  const formatter = new Intl.DateTimeFormat('de-DE', {
    hour: '2-digit',
    minute: '2-digit',
  })

  return `${formatter.format(new Date(slot.startAt))} - ${formatter.format(new Date(slot.endAt))}`
}

export function slotDuration(slot: ScheduleSlot) {
  return Math.round(
    (new Date(slot.endAt).getTime() - new Date(slot.startAt).getTime()) / 60000,
  )
}
