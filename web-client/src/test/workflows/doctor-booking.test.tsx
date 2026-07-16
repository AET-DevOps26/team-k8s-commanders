import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { AppointmentCreate, AuthSession } from '../../clientApi'
import { DoctorBookAppointmentPage } from '../../components/doctor/DoctorBookAppointmentPage'
import {
  availableSlots,
  doctorUser,
  paginated,
  patientUser,
} from '../fixtures'
import { HttpResponse, http, server } from '../server'

const doctorSession: AuthSession = {
  accessToken: 'doctor-access-token',
  user: doctorUser,
}

describe('doctor appointment booking workflow', () => {
  it('books selected patient into one of the doctors own slots', async () => {
    let bookingRequest: AppointmentCreate | null = null
    server.use(
      http.get('*/api/v1/users', () =>
        HttpResponse.json(paginated([doctorUser, patientUser])),
      ),
      http.post('*/api/v1/appointments', async ({ request }) => {
        bookingRequest = (await request.json()) as AppointmentCreate
        return HttpResponse.json(
          {
            id: 'new-appointment',
            ...bookingRequest,
            status: 'SCHEDULED',
          },
          { status: 201 },
        )
      }),
    )
    const onBooked = vi.fn()
    const user = userEvent.setup()

    render(
      <DoctorBookAppointmentPage
        session={doctorSession}
        onLogout={vi.fn()}
        onNavigate={vi.fn()}
        onBooked={onBooked}
      />,
    )

    expect(await screen.findByRole('heading', { name: 'Book for a patient' }))
      .toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: new RegExp(patientUser.name) }))
    const slot = (await screen.findAllByRole('button', {
      name: /\d{2}:\d{2} - \d{2}:\d{2}/,
    }))[0]
    await user.click(slot)
    await user.type(screen.getByLabelText('Reason'), 'Follow-up')
    await user.click(screen.getByRole('button', { name: 'Book selected slot' }))

    await waitFor(() => expect(onBooked).toHaveBeenCalledOnce())
    expect(bookingRequest).toMatchObject({
      patientId: patientUser.id,
      doctorId: doctorUser.id,
      dateTime: availableSlots[0].startAt,
      duration: 60,
      reason: 'Follow-up',
    })
  })

  it('shows load failure without exposing a broken calendar', async () => {
    server.use(
      http.get('*/api/v1/users', () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
    )

    render(
      <DoctorBookAppointmentPage
        session={doctorSession}
        onLogout={vi.fn()}
        onNavigate={vi.fn()}
        onBooked={vi.fn()}
      />,
    )

    expect(
      await screen.findByText('Booking data could not be loaded. Please try again.'),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText('Available day range')).not.toBeInTheDocument()
  })
})
