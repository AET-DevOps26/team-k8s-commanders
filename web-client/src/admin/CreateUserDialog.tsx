import { FormEvent, useState } from 'react'
import type { UserCreate, UserRole } from '../clientApi'
import { ROLE_OPTIONS } from './constants'

type CreateUserDialogProps = {
  onCancel: () => void
  onCreate: (payload: UserCreate) => Promise<void>
}

export function CreateUserDialog({ onCancel, onCreate }: CreateUserDialogProps) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<UserRole>('PATIENT')
  const [specialization, setSpecialization] = useState('')
  const [licenseNumber, setLicenseNumber] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)

  const isDoctor = role === 'DOCTOR'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)

    const payload: UserCreate = { name, email, password, role }
    if (phoneNumber.trim()) {
      payload.phoneNumber = phoneNumber
    }
    if (isDoctor) {
      if (specialization.trim()) {
        payload.specialization = specialization
      }
      if (licenseNumber.trim()) {
        payload.licenseNumber = licenseNumber
      }
    }

    try {
      await onCreate(payload)
    } catch {
      // Error surfaced by the parent via actionError.
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <form className="auth-card modal-card" onSubmit={handleSubmit}>
        <h2>Create user</h2>
        <label>
          Full name
          <input
            onChange={(event) => setName(event.target.value)}
            required
            type="text"
            value={name}
          />
        </label>
        <label>
          Email
          <input
            onChange={(event) => setEmail(event.target.value)}
            required
            type="email"
            value={email}
          />
        </label>
        <label>
          Password
          <input
            autoComplete="new-password"
            minLength={8}
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
            value={password}
          />
        </label>
        <label>
          Role
          <select
            onChange={(event) => setRole(event.target.value as UserRole)}
            value={role}
          >
            {ROLE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <label>
          Phone number (optional)
          <input
            onChange={(event) => setPhoneNumber(event.target.value)}
            type="text"
            value={phoneNumber}
          />
        </label>
        {isDoctor && (
          <>
            <label>
              Specialization
              <input
                onChange={(event) => setSpecialization(event.target.value)}
                type="text"
                value={specialization}
              />
            </label>
            <label>
              License number
              <input
                onChange={(event) => setLicenseNumber(event.target.value)}
                type="text"
                value={licenseNumber}
              />
            </label>
          </>
        )}
        <div className="modal-actions">
          <button className="secondary-button" onClick={onCancel} type="button">
            Cancel
          </button>
          <button
            className="primary-button"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? 'Creating' : 'Create user'}
          </button>
        </div>
      </form>
    </div>
  )
}
