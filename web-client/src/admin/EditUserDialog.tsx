import { FormEvent, useState } from 'react'
import type { UserProfile, UserRole } from '../clientApi'
import { ROLE_OPTIONS } from './constants'

type EditUserDialogProps = {
  user: UserProfile
  busy: boolean
  onCancel: () => void
  onSave: (updated: UserProfile) => Promise<void>
}

export function EditUserDialog({
  user,
  busy,
  onCancel,
  onSave,
}: EditUserDialogProps) {
  const [form, setForm] = useState<UserProfile>({ ...user })
  const [password, setPassword] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const isDoctor = form.role === 'DOCTOR'

  function update<K extends keyof UserProfile>(key: K, value: UserProfile[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const newPassword = password.trim()
    if (newPassword.length > 0 && newPassword.length < 8) {
      return
    }

    setSubmitting(true)

    const payload: UserProfile = { ...form }
    if (newPassword) {
      payload.password = newPassword
    }

    try {
      await onSave(payload)
    } catch (err) {
      console.error('Update user failed', err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <form className="auth-card modal-card" onSubmit={handleSubmit}>
        <h2>Edit {user.name}</h2>
        <label>
          Name
          <input
            onChange={(event) => update('name', event.target.value)}
            required
            type="text"
            value={form.name}
          />
        </label>
        <label>
          Email
          <input
            onChange={(event) => update('email', event.target.value)}
            required
            type="email"
            value={form.email}
          />
        </label>
        <label>
          Role
          <select
            onChange={(event) => {
              const value = event.currentTarget.value
              if ((ROLE_OPTIONS as readonly string[]).includes(value)) {
                update('role', value as UserRole)
              }
            }}
            value={form.role}
          >
            {ROLE_OPTIONS.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </label>
        <label>
          Phone number
          <input
            onChange={(event) => update('phoneNumber', event.target.value)}
            type="text"
            value={form.phoneNumber ?? ''}
          />
        </label>
        {isDoctor && (
          <>
            <label>
              Specialization
              <input
                onChange={(event) => update('specialization', event.target.value)}
                type="text"
                value={form.specialization ?? ''}
              />
            </label>
            <label>
              License number
              <input
                onChange={(event) => update('licenseNumber', event.target.value)}
                type="text"
                value={form.licenseNumber ?? ''}
              />
            </label>
          </>
        )}
        <label>
          Reset password (optional)
          <input
            autoComplete="new-password"
            minLength={8}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Leave blank to keep current"
            type="password"
            value={password}
          />
        </label>
        <label className="checkbox-row">
          <input
            checked={form.enabled !== false}
            onChange={(event) => update('enabled', event.target.checked)}
            type="checkbox"
          />
          Account active
        </label>
        <div className="modal-actions">
          <button className="secondary-button" onClick={onCancel} type="button">
            Cancel
          </button>
          <button
            className="primary-button"
            disabled={busy || isSubmitting}
            type="submit"
          >
            {busy || isSubmitting ? 'Saving' : 'Save changes'}
          </button>
        </div>
      </form>
    </div>
  )
}
