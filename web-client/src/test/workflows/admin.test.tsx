import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { AuthSession, UserCreate, UserProfile } from '../../clientApi'
import { AdminDashboard } from '../../admin/AdminDashboard'
import { HttpResponse, http, server } from '../server'

const admin: UserProfile = {
  id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  name: 'Admin User',
  email: 'admin@caredesk.example',
  role: 'ADMIN',
  enabled: true,
}

const doctor: UserProfile = {
  id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
  name: 'Dr. Existing',
  email: 'doctor@caredesk.example',
  role: 'DOCTOR',
  specialization: 'Cardiology',
  licenseNumber: 'DOC-42',
  enabled: true,
}

const disabledPatient: UserProfile = {
  id: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
  name: 'Disabled Patient',
  email: 'disabled@caredesk.example',
  role: 'PATIENT',
  enabled: false,
}

const session: AuthSession = { accessToken: 'admin-token', user: admin }

function paginated(users: UserProfile[]) {
  return {
    content: users,
    page: {
      page: 0,
      size: users.length,
      totalElements: users.length,
      totalPages: 1,
    },
  }
}

function installAdminApi(initialUsers = [admin, doctor, disabledPatient]) {
  let users = [...initialUsers]
  const created: UserCreate[] = []
  const replaced: UserProfile[] = []

  server.use(
    http.get('*/api/v1/users', () => HttpResponse.json(paginated(users))),
    http.get('*/api/v1/users/stats', () =>
      HttpResponse.json({
        total: users.length,
        patients: users.filter((user) => user.role === 'PATIENT').length,
        doctors: users.filter((user) => user.role === 'DOCTOR').length,
        admins: users.filter((user) => user.role === 'ADMIN').length,
        active: users.filter((user) => user.enabled !== false).length,
        disabled: users.filter((user) => user.enabled === false).length,
      }),
    ),
    http.post('*/api/v1/users', async ({ request }) => {
      const payload = (await request.json()) as UserCreate
      created.push(payload)
      const user: UserProfile = {
        ...payload,
        id: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
        enabled: true,
      }
      users = [...users, user]
      return HttpResponse.json(user, { status: 201 })
    }),
    http.put('*/api/v1/users/:userId', async ({ params, request }) => {
      const payload = (await request.json()) as UserProfile
      const updated = { ...payload, id: String(params.userId) }
      replaced.push(updated)
      users = users.map((user) => (user.id === updated.id ? updated : user))
      return HttpResponse.json(updated)
    }),
  )

  return { created, replaced }
}

function renderDashboard(overrides: Partial<AuthSession> = {}) {
  return render(
    <AdminDashboard
      session={{ ...session, ...overrides }}
      onLogout={vi.fn()}
      onNavigate={vi.fn()}
    />,
  )
}

function last<T>(values: T[]) {
  return values[values.length - 1]
}

describe('admin workflows', () => {
  it('loads users and supports role, activation, and deactivation changes', async () => {
    const user = userEvent.setup()
    const api = installAdminApi()
    renderDashboard()

    expect(await screen.findByText('3 total')).toBeInTheDocument()
    expect(screen.getByText('All accounts')).toBeInTheDocument()

    const doctorRow = screen.getByText(doctor.name).closest('[role="row"]') as HTMLElement
    await user.selectOptions(
      within(doctorRow).getByRole('combobox', { name: `Role for ${doctor.name}` }),
      'PATIENT',
    )
    await waitFor(() => expect(last(api.replaced)?.role).toBe('PATIENT'))

    const disabledRow = screen
      .getByText(disabledPatient.name)
      .closest('[role="row"]') as HTMLElement
    await user.click(within(disabledRow).getByRole('button', { name: 'Activate' }))
    await waitFor(() => expect(last(api.replaced)?.enabled).toBe(true))

    const updatedDoctorRow = screen
      .getByText(doctor.name)
      .closest('[role="row"]') as HTMLElement
    await user.click(
      within(updatedDoctorRow).getByRole('button', { name: 'Deactivate' }),
    )
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: `Deactivate ${doctor.name}?` })).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Deactivate' }))
    await waitFor(() => expect(last(api.replaced)?.enabled).toBe(false))
  })

  it('creates a doctor with optional professional data', async () => {
    const user = userEvent.setup()
    const api = installAdminApi()
    renderDashboard()

    await screen.findByText(doctor.name)
    await user.click(screen.getByRole('button', { name: 'New user' }))
    await user.type(screen.getByLabelText('Full name'), 'Dr. New')
    await user.type(screen.getByLabelText('Email'), 'new@caredesk.example')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.selectOptions(screen.getByLabelText('Role'), 'DOCTOR')
    await user.type(screen.getByLabelText('Phone number (optional)'), '+49 123')
    await user.type(screen.getByLabelText('Specialization'), 'Neurology')
    await user.type(screen.getByLabelText('License number'), 'DOC-99')
    await user.click(screen.getByRole('button', { name: 'Create user' }))

    await waitFor(() => expect(api.created).toHaveLength(1))
    expect(api.created[0]).toMatchObject({
      name: 'Dr. New',
      role: 'DOCTOR',
      phoneNumber: '+49 123',
      specialization: 'Neurology',
      licenseNumber: 'DOC-99',
    })
    expect(await screen.findByText('Dr. New')).toBeInTheDocument()
  })

  it('edits profile data and can cancel dialogs', async () => {
    const user = userEvent.setup()
    const api = installAdminApi()
    renderDashboard()

    const row = (await screen.findByText(doctor.name)).closest('[role="row"]') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Edit' }))
    const name = screen.getByLabelText('Name')
    await user.clear(name)
    await user.type(name, 'Dr. Updated')
    await user.type(screen.getByLabelText('Reset password (optional)'), 'new-password')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(last(api.replaced)?.name).toBe('Dr. Updated'))
    expect(last(api.replaced)?.password).toBe('new-password')

    await user.click(screen.getByRole('button', { name: 'New user' }))
    await user.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('heading', { name: 'Create user' })).not.toBeInTheDocument()
  })

  it('shows API failures and blocks non-admin sessions', async () => {
    server.use(
      http.get('*/api/v1/users', () =>
        HttpResponse.json({ detail: 'Backend unavailable' }, { status: 503 }),
      ),
      http.get('*/api/v1/users/stats', () =>
        HttpResponse.json({ detail: 'Backend unavailable' }, { status: 503 }),
      ),
    )
    renderDashboard()
    expect(await screen.findByText('Backend unavailable')).toBeInTheDocument()

    renderDashboard({ user: { ...doctor, role: 'DOCTOR' } })
    expect(screen.getByRole('heading', { name: 'Admin account required.' })).toBeInTheDocument()
  })
})
