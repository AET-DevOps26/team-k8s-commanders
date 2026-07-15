import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { RecurringScheduleCreate, ScheduleSlotCreate } from '../../clientApi'
import { HttpResponse, http, server } from '../../test/server'
import { AvailabilitySlotCreator } from './AvailabilitySlotCreator'
import { AvailabilitySlotsPanel } from './AvailabilitySlotsPanel'

const doctorId = '22222222-2222-4222-8222-222222222222'

function localDate(daysFromNow: number) {
  const date = new Date()
  date.setDate(date.getDate() + daysFromNow)
  return date.toISOString().slice(0, 10)
}

function localDateTime(daysFromNow: number, hour: number) {
  return `${localDate(daysFromNow)}T${String(hour).padStart(2, '0')}:00`
}

describe('availability management', () => {
  afterEach(() => vi.restoreAllMocks())

  it('publishes one future slot with selected duration', async () => {
    const payloads: ScheduleSlotCreate[] = []
    const onCreated = vi.fn()
    server.use(
      http.post(`*/api/v1/doctors/${doctorId}/schedule`, async ({ request }) => {
        const payload = (await request.json()) as ScheduleSlotCreate
        payloads.push(payload)
        return HttpResponse.json({
          id: 'new-slot',
          ...payload,
          available: true,
        })
      }),
    )
    render(
      <AvailabilitySlotCreator
        doctorId={doctorId}
        token="token"
        onCreated={onCreated}
        onCreatedMany={vi.fn()}
      />,
    )

    fireEvent.change(screen.getByLabelText('Start'), {
      target: { value: localDateTime(2, 10) },
    })
    await userEvent.selectOptions(screen.getByLabelText('Duration'), '45')
    await userEvent.click(screen.getByRole('button', { name: 'Publish availability' }))

    await waitFor(() => expect(onCreated).toHaveBeenCalledOnce())
    expect(new Date(payloads[0].endAt).getTime() - new Date(payloads[0].startAt).getTime())
      .toBe(45 * 60_000)
    expect(screen.getByText('Availability published.')).toBeInTheDocument()
  })

  it('validates single and recurring input before sending requests', async () => {
    const user = userEvent.setup()
    render(
      <AvailabilitySlotCreator
        doctorId={doctorId}
        token="token"
        onCreated={vi.fn()}
        onCreatedMany={vi.fn()}
      />,
    )
    const form = screen.getByRole('button', { name: 'Publish availability' }).closest('form')!

    fireEvent.change(screen.getByLabelText('Start'), {
      target: { value: localDateTime(-1, 10) },
    })
    fireEvent.submit(form)
    expect(screen.getByText('Choose a future time.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Weekly' }))
    fireEvent.submit(form)
    expect(screen.getByText('Pick at least one weekday.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Mon' }))
    fireEvent.change(screen.getByLabelText('Until'), { target: { value: '08:00' } })
    fireEvent.submit(form)
    expect(screen.getByText('The end time must be after the start time.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Until'), { target: { value: '09:15' } })
    await user.selectOptions(screen.getByLabelText('Slot length'), '30')
    fireEvent.submit(form)
    expect(screen.getByText('The time window is shorter than one slot.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Until'), { target: { value: '10:00' } })
    fireEvent.submit(form)
    expect(screen.getByText('Choose a first and last day.')).toBeInTheDocument()
  })

  it('publishes recurring slots and reports skipped conflicts', async () => {
    const user = userEvent.setup()
    const payloads: RecurringScheduleCreate[] = []
    const onCreatedMany = vi.fn()
    const startDate = localDate(3)
    const endDate = localDate(10)
    server.use(
      http.post(
        `*/api/v1/doctors/${doctorId}/schedule/recurring`,
        async ({ request }) => {
          const payload = (await request.json()) as RecurringScheduleCreate
          payloads.push(payload)
          return HttpResponse.json({
            created: [{
              id: 'recurring-slot',
              startAt: `${startDate}T09:00:00.000Z`,
              endAt: `${startDate}T09:30:00.000Z`,
              available: true,
            }],
            skipped: [{ startAt: `${endDate}T09:00:00.000Z`, reason: 'OVERLAP' }],
          })
        },
      ),
    )
    render(
      <AvailabilitySlotCreator
        doctorId={doctorId}
        token="token"
        onCreated={vi.fn()}
        onCreatedMany={onCreatedMany}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Weekly' }))
    await user.click(screen.getByRole('button', { name: 'Mon' }))
    await user.click(screen.getByRole('button', { name: 'Wed' }))
    fireEvent.change(screen.getByLabelText('First day'), { target: { value: startDate } })
    fireEvent.change(screen.getByLabelText('Last day'), { target: { value: endDate } })
    expect(screen.getByText(/slots across/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Publish availability' }))

    await waitFor(() => expect(onCreatedMany).toHaveBeenCalledOnce())
    expect(payloads[0]).toMatchObject({
      weekdays: ['MONDAY', 'WEDNESDAY'],
      startDate,
      endDate,
      slotDurationMinutes: 30,
    })
    expect(screen.getByText('Published 1 slot. Skipped 1 overlapping with existing slots.'))
      .toBeInTheDocument()
  })

  it('shows publishing failures without adding slots', async () => {
    const onCreated = vi.fn()
    server.use(
      http.post(`*/api/v1/doctors/${doctorId}/schedule`, () =>
        HttpResponse.json({ detail: 'Conflict' }, { status: 409 }),
      ),
    )
    render(
      <AvailabilitySlotCreator
        doctorId={doctorId}
        token="token"
        onCreated={onCreated}
        onCreatedMany={vi.fn()}
      />,
    )
    fireEvent.change(screen.getByLabelText('Start'), {
      target: { value: localDateTime(2, 10) },
    })
    await userEvent.click(screen.getByRole('button', { name: 'Publish availability' }))

    expect(await screen.findByText('Availability could not be published. Check the time range.'))
      .toBeInTheDocument()
    expect(onCreated).not.toHaveBeenCalled()
  })

  it('filters old slots and removes only after confirmation', async () => {
    const user = userEvent.setup()
    const onDelete = vi.fn().mockResolvedValue(undefined)
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    render(
      <AvailabilitySlotsPanel
        slots={[
          { id: 'past', startAt: localDateTime(-2, 9), endAt: localDateTime(-2, 10), available: true },
          { id: 'future', startAt: localDateTime(2, 9), endAt: localDateTime(2, 10), available: true },
          { id: 'booked', startAt: localDateTime(3, 9), endAt: localDateTime(3, 10), available: false },
        ]}
        onDelete={onDelete}
      />,
    )

    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('Booked')).toBeInTheDocument()
    const remove = screen.getByRole('button', { name: 'Remove' })
    await user.click(remove)
    expect(onDelete).not.toHaveBeenCalled()
    await user.click(remove)
    await waitFor(() => expect(onDelete).toHaveBeenCalledWith('future'))
    expect(confirm).toHaveBeenCalledTimes(2)
  })

  it('renders an empty state without future slots', () => {
    render(<AvailabilitySlotsPanel slots={[]} onDelete={vi.fn()} />)
    expect(screen.getByText('No future slots published.')).toBeInTheDocument()
  })
})
