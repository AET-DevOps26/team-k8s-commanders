import type { Appointment } from '../../clientApi'

export type AppointmentSortOrder = 'newest' | 'oldest'
export type AppointmentStatusFilter = 'ALL' | Appointment['status']

export const appointmentStatusFilters: Array<{
  value: AppointmentStatusFilter
  label: string
}> = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'RESCHEDULED', label: 'Rescheduled' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]
