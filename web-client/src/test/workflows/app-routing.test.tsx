import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '../../App'
import type { AuthSession, UserProfile } from '../../clientApi'
import { SESSION_KEY } from '../../constants/session'
import { doctorUser, paginated, patientUser } from '../fixtures'
import { HttpResponse, http, server } from '../server'

const doctorSession: AuthSession = { accessToken: 'doctor-token', user: doctorUser }
const adminUser: UserProfile = {
  id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  name: 'Admin User',
  email: 'admin@caredesk.example',
  role: 'ADMIN',
  enabled: true,
}
const adminSession: AuthSession = { accessToken: 'admin-token', user: adminUser }

function renderStoredSession(path: string, session: AuthSession) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  window.history.replaceState({}, '', path)
  return render(<App />)
}

describe('application role routing', () => {
  it('redirects doctor away from patient routes', async () => {
    server.use(
      http.get('*/api/v1/appointments', () => HttpResponse.json(paginated([]))),
      http.get('*/api/v1/users', () => HttpResponse.json(paginated([doctorUser]))),
    )
    renderStoredSession('/patient/profile', doctorSession)
    expect(await screen.findByRole('heading', { name: doctorUser.name })).toBeInTheDocument()
  })

  it('redirects admin away from patient routes', async () => {
    server.use(
      http.get('*/api/v1/users', () => HttpResponse.json(paginated([adminUser]))),
      http.get('*/api/v1/users/stats', () => HttpResponse.json({
        total: 1,
        patients: 0,
        doctors: 0,
        admins: 1,
        active: 1,
        disabled: 0,
      })),
    )
    renderStoredSession('/patient/book', adminSession)
    expect(await screen.findByRole('heading', { name: 'User management' })).toBeInTheDocument()
  })

  it('lets component guard reject patient on doctor route', () => {
    server.use(
      http.get('*/api/v1/appointments', () => HttpResponse.json(paginated([]))),
      http.get('*/api/v1/users', () => HttpResponse.json(paginated([patientUser]))),
    )
    renderStoredSession('/doctor', { accessToken: 'patient-token', user: patientUser })
    expect(screen.getByRole('heading', { name: 'Doctor account required.' })).toBeInTheDocument()
  })

  it.each(['/admin', '/doctor', '/doctor/schedule', '/doctor/patients', '/doctor/book'])(
    'requires login for protected route %s',
    (path) => {
      window.history.replaceState({}, '', path)
      render(<App />)
      expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeInTheDocument()
    },
  )

  it('reacts to browser popstate navigation', async () => {
    renderStoredSession('/', { accessToken: 'patient-token', user: patientUser })
    window.history.pushState({}, '', '/patient/profile')
    window.dispatchEvent(new PopStateEvent('popstate'))
    expect(await screen.findByRole('heading', { name: 'Account settings' })).toBeInTheDocument()
  })
})
