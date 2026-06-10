import { FormEvent, useState } from 'react'
import { login, registerPatient } from '../../clientApi'
import { userMessage } from '../../lib/messages'
import type { AuthFormProps } from '../../types/route'
import { ShellNav } from '../layout/ShellNav'

export function AuthForm({ mode, onAuthenticated, onNavigate }: AuthFormProps) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const isRegister = mode === 'register'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const session = isRegister
        ? await registerPatient({ name, email, password, phoneNumber, dateOfBirth })
        : await login({ email, password })

      onAuthenticated(session)
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? userMessage(isRegister ? 'Account could not be created. Please check your details and try again.' : 'Login failed. Please check your email and password.')
          : userMessage('Something went wrong. Please try again.'),
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="landing-page auth-page">
      <ShellNav session={null} onNavigate={onNavigate} />
      <section className="auth-shell">
        <div className="auth-copy">
          <p className="eyebrow">Patient access</p>
          <h1>{isRegister ? 'Create patient account.' : 'Welcome back.'}</h1>
          <p>
            Welcome to CareDesk. Sign in and discover your health information at
            a glance.
          </p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <h2>{isRegister ? 'Sign up' : 'Login'}</h2>
          {isRegister && (
            <>
              <label>
                Full name
                <input
                  autoComplete="name"
                  onChange={(event) => setName(event.target.value)}
                  required
                  type="text"
                  value={name}
                />
              </label>
              <label>
                Phone number
                <input
                  autoComplete="tel"
                  onChange={(event) => setPhoneNumber(event.target.value)}
                  required
                  type="tel"
                  value={phoneNumber}
                />
              </label>
              <label>
                Date of birth
                <input
                  onChange={(event) => setDateOfBirth(event.target.value)}
                  required
                  type="date"
                  value={dateOfBirth}
                />
              </label>
            </>
          )}
          <label>
            Email
            <input
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </label>
          <label>
            Password
            <input
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              minLength={8}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </label>
          {error && <p className="form-error">{error}</p>}
          <button className="primary-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Please wait' : isRegister ? 'Create account' : 'Login'}
          </button>
          <button
            className="text-link button-reset"
            onClick={() => onNavigate(isRegister ? '/login' : '/register')}
            type="button"
          >
            {isRegister ? 'Already have an account?' : 'Need an account?'}
          </button>
        </form>
      </section>
    </main>
  )
}
