import {
  appointmentStatusFilters,
  type AppointmentSortOrder,
  type AppointmentStatusFilter,
} from './appointmentFilters'

export type { AppointmentSortOrder, AppointmentStatusFilter } from './appointmentFilters'

type AppointmentFilterBarProps = {
  statusFilter: AppointmentStatusFilter
  sortOrder: AppointmentSortOrder
  onStatusFilterChange: (value: AppointmentStatusFilter) => void
  onSortOrderChange: (value: AppointmentSortOrder) => void
}

/** Status + sort controls shared by the doctor and patient appointment views. */
export function AppointmentFilterBar({
  statusFilter,
  sortOrder,
  onStatusFilterChange,
  onSortOrderChange,
}: AppointmentFilterBarProps) {
  return (
    <div className="appointment-filter-bar">
      <label className="appointment-filter-field">
        <span>Status</span>
        <select
          onChange={(event) =>
            onStatusFilterChange(event.target.value as AppointmentStatusFilter)
          }
          value={statusFilter}
        >
          {appointmentStatusFilters.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      <label className="appointment-filter-field">
        <span>Sort</span>
        <select
          onChange={(event) =>
            onSortOrderChange(event.target.value as AppointmentSortOrder)
          }
          value={sortOrder}
        >
          <option value="newest">Newest first</option>
          <option value="oldest">Oldest first</option>
        </select>
      </label>
    </div>
  )
}
