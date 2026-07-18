import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { PasswordChangeRequest, UserProfile } from '../../clientApi'
import { PatientProfilePage } from '../../components/patient/PatientProfilePage'
import { patientSession, patientUser } from '../fixtures'
import { HttpResponse, http, server } from '../server'

function renderProfile(onSessionUpdated = vi.fn()) {
  render(
    <PatientProfilePage
      session={patientSession}
      onLogout={vi.fn()}
      onNavigate={vi.fn()}
      onSessionUpdated={onSessionUpdated}
    />,
  )
  return onSessionUpdated
}

describe('patient profile', () => {
  it('loads and saves personal data into session state', async () => {
    const user = userEvent.setup()
    const updates: UserProfile[] = []
    const loaded = { ...patientUser, name: 'Loaded Name' }
    server.use(
      http.get(`*/api/v1/users/${patientUser.id}`, () => HttpResponse.json(loaded)),
      http.put(`*/api/v1/users/${patientUser.id}`, async ({ request }) => {
        const payload = (await request.json()) as UserProfile
        updates.push(payload)
        return HttpResponse.json(payload)
      }),
    )
    const onSessionUpdated = renderProfile()

    const name = await screen.findByDisplayValue('Loaded Name')
    await user.clear(name)
    await user.type(name, 'Anna Updated')
    await user.clear(screen.getByLabelText('Phone number'))
    await user.type(screen.getByLabelText('Phone number'), '+49 999')
    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    await waitFor(() => expect(updates).toHaveLength(1))
    expect(updates[0]).toMatchObject({ name: 'Anna Updated', phoneNumber: '+49 999' })
    expect(onSessionUpdated).toHaveBeenCalledWith(
      expect.objectContaining({ user: expect.objectContaining({ name: 'Anna Updated' }) }),
    )
    expect(screen.getByText('Profile updated')).toBeInTheDocument()
  })

  it('changes password and clears sensitive fields', async () => {
    const user = userEvent.setup()
    const changes: PasswordChangeRequest[] = []
    server.use(
      http.put(`*/api/v1/users/${patientUser.id}/password`, async ({ request }) => {
        changes.push((await request.json()) as PasswordChangeRequest)
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderProfile()
    await screen.findByDisplayValue(patientUser.name)

    const current = screen.getByLabelText('Current password')
    const next = screen.getByLabelText('New password')
    await user.type(current, 'current-password')
    await user.type(next, 'new-password')
    await user.click(screen.getByRole('button', { name: 'Change password' }))

    await waitFor(() => expect(changes).toEqual([{
      currentPassword: 'current-password',
      newPassword: 'new-password',
    }]))
    expect(current).toHaveValue('')
    expect(next).toHaveValue('')
    expect(screen.getByText('Password changed')).toBeInTheDocument()
  })

  it('shows load and mutation failures', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(`*/api/v1/users/${patientUser.id}`, () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
      http.put(`*/api/v1/users/${patientUser.id}`, () =>
        HttpResponse.json({ detail: 'Rejected' }, { status: 400 }),
      ),
      http.put(`*/api/v1/users/${patientUser.id}/password`, () =>
        HttpResponse.json({ detail: 'Wrong password' }, { status: 400 }),
      ),
    )
    renderProfile()

    expect(await screen.findByText(/profile could not be loaded/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Save profile' }))
    expect(await screen.findByText(/changes could not be saved/)).toBeInTheDocument()

    await user.type(screen.getByLabelText('Current password'), 'wrong-pass')
    await user.type(screen.getByLabelText('New password'), 'new-password')
    await user.click(screen.getByRole('button', { name: 'Change password' }))
    expect(await screen.findByText(/password could not be changed/)).toBeInTheDocument()
  })
})
