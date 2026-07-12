import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../../App'
import { SESSION_KEY } from '../../constants/session'
import {
  PATIENT_PASSWORD,
  patientUser,
  seedStoredSession,
} from '../fixtures'
import { HttpResponse, http, server } from '../server'

function renderAppAt(path: string) {
  window.history.replaceState({}, '', path)
  return render(<App />)
}

describe('login workflow', () => {
  it('lets a returning patient log in from the landing page and see their dashboard', async () => {
    const user = userEvent.setup()
    renderAppAt('/')

    // Landing page → login form
    expect(
      screen.getByRole('heading', { name: /one clear desk/i }),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Log in' }))
    expect(
      await screen.findByRole('heading', { name: 'Welcome back.' }),
    ).toBeInTheDocument()

    // Fill credentials and submit
    await user.type(screen.getByLabelText('Email'), patientUser.email)
    await user.type(screen.getByLabelText('Password'), PATIENT_PASSWORD)
    await user.click(screen.getByRole('button', { name: 'Login' }))

    // Patient dashboard renders live data from the API
    expect(
      await screen.findByRole('heading', { name: patientUser.name }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Annual check-up')).toBeInTheDocument()
    expect(screen.getByText(patientUser.email)).toBeInTheDocument()

    // Session is persisted for the next visit
    expect(window.localStorage.getItem(SESSION_KEY)).toContain(
      patientUser.email,
    )
    expect(window.location.pathname).toBe('/patient')
  })

  it('shows an error and stays on the form when credentials are wrong', async () => {
    const user = userEvent.setup()
    renderAppAt('/login')

    await user.type(screen.getByLabelText('Email'), patientUser.email)
    await user.type(screen.getByLabelText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Login' }))

    expect(
      await screen.findByText(
        'Login failed. Please check your email and password.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Login' })).toBeEnabled()
    expect(window.localStorage.getItem(SESSION_KEY)).toBeNull()
  })
})

describe('registration workflow', () => {
  it('registers a new patient and lands on the patient dashboard', async () => {
    const user = userEvent.setup()
    renderAppAt('/')

    await user.click(screen.getByRole('link', { name: 'Sign up' }))
    expect(
      await screen.findByRole('heading', { name: 'Create patient account.' }),
    ).toBeInTheDocument()

    await user.type(screen.getByLabelText('Full name'), 'Anna Beispiel')
    await user.type(screen.getByLabelText('Phone number'), '+49 170 1234567')
    await user.type(screen.getByLabelText('Date of birth'), '1990-04-12')
    await user.type(screen.getByLabelText('Email'), patientUser.email)
    await user.type(screen.getByLabelText('Password'), PATIENT_PASSWORD)
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(
      await screen.findByRole('heading', { name: patientUser.name }),
    ).toBeInTheDocument()
    expect(window.location.pathname).toBe('/patient')
  })

  it('shows an error when registration is rejected', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json({ detail: 'Email already registered' }, { status: 409 }),
      ),
    )

    const user = userEvent.setup()
    renderAppAt('/register')

    await user.type(screen.getByLabelText('Full name'), 'Anna Beispiel')
    await user.type(screen.getByLabelText('Phone number'), '+49 170 1234567')
    await user.type(screen.getByLabelText('Date of birth'), '1990-04-12')
    await user.type(screen.getByLabelText('Email'), patientUser.email)
    await user.type(screen.getByLabelText('Password'), PATIENT_PASSWORD)
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(
      await screen.findByText(
        'Account could not be created. Please check your details and try again.',
      ),
    ).toBeInTheDocument()
  })
})

describe('session persistence', () => {
  it('restores a stored session and shows the dashboard without logging in again', async () => {
    seedStoredSession()
    renderAppAt('/patient')

    expect(
      await screen.findByRole('heading', { name: patientUser.name }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Welcome back.' }),
    ).not.toBeInTheDocument()
  })

  it('asks for login when visiting the dashboard without a session', () => {
    renderAppAt('/patient')

    expect(
      screen.getByRole('heading', { name: 'Welcome back.' }),
    ).toBeInTheDocument()
  })

  it('ignores a corrupted stored session and asks for login', () => {
    window.localStorage.setItem(SESSION_KEY, '{broken json')
    renderAppAt('/patient')

    expect(
      screen.getByRole('heading', { name: 'Welcome back.' }),
    ).toBeInTheDocument()
  })
})

describe('logout workflow', () => {
  it('logs the patient out, clears the session, and returns to the landing page', async () => {
    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient')

    await screen.findByRole('heading', { name: patientUser.name })
    await user.click(screen.getByRole('button', { name: 'Logout' }))

    expect(
      await screen.findByRole('heading', { name: /one clear desk/i }),
    ).toBeInTheDocument()
    expect(window.localStorage.getItem(SESSION_KEY)).toBeNull()
    expect(window.location.pathname).toBe('/')
  })

  it('still logs out locally when the backend logout call fails', async () => {
    server.use(
      http.post(
        '*/api/v1/auth/logout',
        () => new HttpResponse(null, { status: 503 }),
      ),
    )

    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient')

    await screen.findByRole('heading', { name: patientUser.name })
    await user.click(screen.getByRole('button', { name: 'Logout' }))

    await waitFor(() => {
      expect(window.localStorage.getItem(SESSION_KEY)).toBeNull()
    })
    expect(
      await screen.findByRole('heading', { name: /one clear desk/i }),
    ).toBeInTheDocument()
  })
})
