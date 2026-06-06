import type { UserProfile, UserRole } from '../clientApi'
import { ROLE_OPTIONS } from './constants'
import type { AdminData } from './types'

type UserTableProps = {
  data: AdminData
  currentUserId: string
  busyId: string | null
  isLoading: boolean
  onEdit: (user: UserProfile) => void
  onRoleChange: (user: UserProfile, role: UserRole) => void
  onDeactivate: (user: UserProfile) => void
  onActivate: (user: UserProfile) => void
  onPageChange: (page: number) => void
}

export function UserTable({
  data,
  currentUserId,
  busyId,
  isLoading,
  onEdit,
  onRoleChange,
  onDeactivate,
  onActivate,
  onPageChange,
}: UserTableProps) {
  return (
    <article className="dashboard-panel wide-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Users</p>
          <h2>{data.total} total</h2>
        </div>
      </div>

      <div className="admin-table" role="table">
        <div className="admin-row admin-head" role="row">
          <span>Name</span>
          <span>Email</span>
          <span>Role</span>
          <span>Status</span>
          <span>Actions</span>
        </div>
        {data.users.map((user) => (
          <div className="admin-row" role="row" key={user.id}>
            <span data-label="Name">{user.name}</span>
            <span data-label="Email">{user.email}</span>
            <span data-label="Role">
              <select
                aria-label={`Role for ${user.name}`}
                disabled={busyId === user.id || user.id === currentUserId}
                onChange={(event) =>
                  onRoleChange(user, event.target.value as UserRole)
                }
                value={user.role}
              >
                {ROLE_OPTIONS.map((role) => (
                  <option key={role} value={role}>
                    {role}
                  </option>
                ))}
              </select>
            </span>
            <span data-label="Status">
              <span
                className={`status-pill ${
                  user.enabled === false ? 'is-disabled' : 'is-active'
                }`}
              >
                {user.enabled === false ? 'Disabled' : 'Active'}
              </span>
            </span>
            <span data-label="Actions" className="admin-actions">
              <button
                className="text-link button-reset"
                onClick={() => onEdit(user)}
                type="button"
              >
                Edit
              </button>
              {user.enabled === false ? (
                <button
                  className="text-link button-reset"
                  disabled={busyId === user.id}
                  onClick={() => onActivate(user)}
                  type="button"
                >
                  Activate
                </button>
              ) : (
                <button
                  className="text-link button-reset danger"
                  disabled={busyId === user.id || user.id === currentUserId}
                  onClick={() => onDeactivate(user)}
                  type="button"
                >
                  Deactivate
                </button>
              )}
            </span>
          </div>
        ))}
      </div>

      <div className="admin-pagination">
        <button
          className="secondary-button"
          disabled={data.page <= 0 || isLoading}
          onClick={() => onPageChange(Math.max(0, data.page - 1))}
          type="button"
        >
          Previous
        </button>
        <span>
          Page {data.page + 1} of {Math.max(1, data.totalPages)}
        </span>
        <button
          className="secondary-button"
          disabled={data.page + 1 >= data.totalPages || isLoading}
          onClick={() => onPageChange(data.page + 1)}
          type="button"
        >
          Next
        </button>
      </div>
    </article>
  )
}
