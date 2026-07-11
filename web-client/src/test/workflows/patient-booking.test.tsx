import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../../App'
import type { AppointmentCreate } from '../../clientApi'
import {
  availableSlots,
  doctorUser,
  patientUser,
  seedStoredSession,
} from '../fixtures'
import { HttpResponse, http, server } from '../server'

function renderAppAt(path: string) {
  window.history.replaceState({}, '', path)
  return render(<App />)
}

describe('appointment booking workflow', () => {
  it('books an appointment: search doctor → pick slot → confirm → success on dashboard', async () => {
    let bookingRequest: AppointmentCreate | null = null
    server.use(
      http.post('*/api/v1/appointments', async ({ request }) => {
        bookingRequest = (await request.json()) as AppointmentCreate

        return HttpResponse.json(
          {
            id: '66666666-6666-4666-8666-666666666666',
            ...bookingRequest,
            status: 'SCHEDULED',
          },
          { status: 201 },
        )
      }),
    )

    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient')

    // Dashboard → booking page via sub navigation
    await screen.findByRole('heading', { name: patientUser.name })
    await user.click(screen.getByRole('button', { name: 'Book appointment' }))
    expect(
      await screen.findByRole('heading', { name: 'Find a doctor' }),
    ).toBeInTheDocument()

    // Initial doctor search loads automatically; select the doctor
    const doctorCard = await screen.findByRole('button', {
      name: new RegExp(doctorUser.name),
    })
    expect(within(doctorCard).getByText('General Medicine')).toBeInTheDocument()
    await user.click(doctorCard)

    // Calendar shows the doctor's available slots; pick the first one
    expect(
      await screen.findByRole('heading', { name: doctorUser.name }),
    ).toBeInTheDocument()
    const slotButtons = await screen.findAllByRole('button', {
      name: /\d{2}:\d{2} - \d{2}:\d{2}/,
    })
    expect(slotButtons).toHaveLength(availableSlots.length)
    await user.click(slotButtons[0])

    // Add a reason and book
    await user.type(screen.getByLabelText('Reason'), 'Recurring headaches')
    await user.click(screen.getByRole('button', { name: 'Book selected slot' }))

    // Back on the dashboard with a success message
    expect(
      await screen.findByText('Appointment booked successfully.'),
    ).toBeInTheDocument()
    expect(window.location.pathname).toBe('/patient')

    // The API received the workflow's selections
    await waitFor(() => expect(bookingRequest).not.toBeNull())
    expect(bookingRequest).toMatchObject({
      patientId: patientUser.id,
      doctorId: doctorUser.id,
      dateTime: availableSlots[0].startAt,
      duration: 60,
      reason: 'Recurring headaches',
    })
  })

  it('requires a slot before booking', async () => {
    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient/book')

    const doctorCard = await screen.findByRole('button', {
      name: new RegExp(doctorUser.name),
    })
    await user.click(doctorCard)
    await screen.findAllByRole('button', { name: /\d{2}:\d{2} - \d{2}:\d{2}/ })

    await user.click(screen.getByRole('button', { name: 'Book selected slot' }))

    expect(
      await screen.findByText('Please select a doctor and time slot'),
    ).toBeInTheDocument()
  })

  it('shows an error when the slot was taken in the meantime', async () => {
    server.use(
      http.post('*/api/v1/appointments', () =>
        HttpResponse.json({ detail: 'Slot already booked' }, { status: 409 }),
      ),
    )

    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient/book')

    const doctorCard = await screen.findByRole('button', {
      name: new RegExp(doctorUser.name),
    })
    await user.click(doctorCard)
    const slotButtons = await screen.findAllByRole('button', {
      name: /\d{2}:\d{2} - \d{2}:\d{2}/,
    })
    await user.click(slotButtons[0])
    await user.click(screen.getByRole('button', { name: 'Book selected slot' }))

    expect(
      await screen.findByText(
        'This appointment could not be booked. Please choose another time or try again.',
      ),
    ).toBeInTheDocument()
    // Still on the booking page so the patient can retry
    expect(window.location.pathname).toBe('/patient/book')
  })

  it('shows an empty state when no doctors match the search', async () => {
    server.use(
      http.get('*/api/v1/doctors', ({ request }) => {
        const url = new URL(request.url)

        return HttpResponse.json({
          content: url.searchParams.get('q') ? [] : [doctorUser],
          page: { page: 0, size: 0, totalElements: 0, totalPages: 0 },
        })
      }),
    )

    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient/book')

    await screen.findByRole('button', { name: new RegExp(doctorUser.name) })

    await user.type(
      screen.getByPlaceholderText('Name or specialization'),
      'Unknown Doctor',
    )
    await user.click(screen.getByRole('button', { name: 'Search doctors' }))

    expect(
      await screen.findByText('No doctors found. Try another search.'),
    ).toBeInTheDocument()
  })
})

describe('appointment management workflow', () => {
  it('cancels an upcoming appointment from the dashboard', async () => {
    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient')

    await screen.findByRole('heading', { name: patientUser.name })

    const appointmentItem = screen
      .getByText('Annual check-up')
      .closest('.appointment-item') as HTMLElement
    await user.click(
      within(appointmentItem).getByRole('button', { name: 'Cancel' }),
    )

    expect(
      await screen.findByText('Appointment cancelled'),
    ).toBeInTheDocument()
  })

  it('reschedules an upcoming appointment to a new slot', async () => {
    const user = userEvent.setup()
    seedStoredSession()
    renderAppAt('/patient')

    await screen.findByRole('heading', { name: patientUser.name })

    const appointmentItem = screen
      .getByText('Annual check-up')
      .closest('.appointment-item') as HTMLElement
    await user.click(
      within(appointmentItem).getByRole('button', { name: 'Move' }),
    )

    const slotOptions = await within(appointmentItem).findAllByRole('button', {
      name: /\d{2}:\d{2} - \d{2}:\d{2}/,
    })
    await user.click(slotOptions[0])
    await user.click(
      within(appointmentItem).getByRole('button', { name: 'Confirm new time' }),
    )

    expect(await screen.findByText('Appointment moved')).toBeInTheDocument()
  })
})
