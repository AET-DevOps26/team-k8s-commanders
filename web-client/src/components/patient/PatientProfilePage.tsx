import { FormEvent, useEffect, useState } from 'react'
import type { UserProfile } from '../../clientApi'
import {
  changeUserPassword,
  getUserProfile,
  updateUserProfile,
} from '../../clientApi'
import { userMessage } from '../../lib/messages'
import { saveSession } from '../../lib/session'
import type { PatientProfileProps } from '../../types/route'
import { PatientSubNav } from '../layout/PatientSubNav'
import { ShellNav } from '../layout/ShellNav'
import { StatusPanel } from '../ui/StatusPanel'

export function PatientProfilePage({
  session,
  onLogout,
  onNavigate,
  onSessionUpdated,
}: PatientProfileProps) {
  const [profile, setProfile] = useState<UserProfile>(session.user)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [profileStatus, setProfileStatus] = useState('')
  const [passwordStatus, setPasswordStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const userId = session.user.id

  useEffect(() => {
    let isActive = true

    async function loadProfile() {
      setLoading(true)
      setError('')

      try {
        const loadedProfile = await getUserProfile(userId, session.accessToken)

        if (isActive) {
          setProfile(loadedProfile)
        }
      } catch {
        if (isActive) {
          setError(userMessage('Your profile could not be loaded. Please try again in a moment.'))
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      isActive = false
    }
  }, [session.accessToken, userId])

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setProfileStatus('')

    try {
      const updatedProfile = await updateUserProfile(userId, session.accessToken, profile)
      const updatedSession = { ...session, user: updatedProfile }
      saveSession(updatedSession)
      onSessionUpdated(updatedSession)
      setProfile(updatedProfile)
      setProfileStatus('Profile updated')
    } catch {
      setError(userMessage('Your changes could not be saved. Please check your details and try again.'))
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setPasswordStatus('')

    try {
      await changeUserPassword(userId, session.accessToken, {
        currentPassword,
        newPassword,
      })
      setCurrentPassword('')
      setNewPassword('')
      setPasswordStatus('Password changed')
    } catch {
      setError(userMessage('Your password could not be changed. Please check your current password.'))
    }
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <PatientSubNav active="profile" onNavigate={onNavigate} />
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Profile</p>
            <h1>Account settings</h1>
            <p>Manage personal data, email, and password for your CareDesk account.</p>
          </div>
        </header>

        {isLoading && <StatusPanel title="Loading your profile" />}
        {error && <StatusPanel title="We could not update your account" text={error} />}

        <section className="settings-grid">
          <form className="auth-card settings-card" onSubmit={handleProfileSubmit}>
            <h2>Personal data</h2>
            <label>
              Full name
              <input
                autoComplete="name"
                onChange={(event) => setProfile({ ...profile, name: event.target.value })}
                required
                type="text"
                value={profile.name}
              />
            </label>
            <label>
              Email
              <input
                autoComplete="email"
                onChange={(event) => setProfile({ ...profile, email: event.target.value })}
                required
                type="email"
                value={profile.email}
              />
            </label>
            <label>
              Phone number
              <input
                autoComplete="tel"
                onChange={(event) => setProfile({ ...profile, phoneNumber: event.target.value })}
                type="tel"
                value={profile.phoneNumber ?? ''}
              />
            </label>
            <label>
              Date of birth
              <input
                onChange={(event) => setProfile({ ...profile, dateOfBirth: event.target.value })}
                type="date"
                value={profile.dateOfBirth ?? ''}
              />
            </label>
            <label>
              Role
              <input readOnly type="text" value={profile.role} />
            </label>
            {profileStatus && <p className="form-success">{profileStatus}</p>}
            <button className="primary-button" type="submit">
              Save profile
            </button>
          </form>

          <form className="auth-card settings-card" onSubmit={handlePasswordSubmit}>
            <h2>Password</h2>
            <label>
              Current password
              <input
                autoComplete="current-password"
                minLength={8}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
                type="password"
                value={currentPassword}
              />
            </label>
            <label>
              New password
              <input
                autoComplete="new-password"
                minLength={8}
                onChange={(event) => setNewPassword(event.target.value)}
                required
                type="password"
                value={newPassword}
              />
            </label>
            {passwordStatus && <p className="form-success">{passwordStatus}</p>}
            <button className="primary-button" type="submit">
              Change password
            </button>
          </form>
        </section>
      </section>
    </main>
  )
}
