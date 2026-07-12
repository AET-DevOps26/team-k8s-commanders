import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type { ScheduleSlot } from '../../clientApi'
import { availableSlots, isoDaysFromNow } from '../../test/fixtures'
import { BookingCalendarSection } from './BookingCalendarSection'

function BookingCalendarHarness({ slots }: { slots: ScheduleSlot[] }) {
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)

  return (
    <BookingCalendarSection
      bookLabel="Book selected slot"
      cancelLabel="Cancel"
      onBook={vi.fn()}
      onCancel={vi.fn()}
      onReasonChange={vi.fn()}
      onSelectSlot={setSelectedSlot}
      reason=""
      selectedSlot={selectedSlot}
      slots={slots}
      subjectName="Dr. Test"
    />
  )
}

function minutesAfter(isoDate: string, minutes: number) {
  return new Date(new Date(isoDate).getTime() + minutes * 60000).toISOString()
}

describe('BookingCalendarSection', () => {
  it('keeps day navigation available after selecting a slot', async () => {
    const user = userEvent.setup()
    render(<BookingCalendarHarness slots={availableSlots} />)

    await user.click(
      await screen.findByRole('button', { name: /09:00 - 10:00/ }),
    )

    const dayButtons = await screen.findAllByRole('button', { name: /1 slots/ })
    await user.click(dayButtons[1])

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /14:00 - 15:00/ }),
      ).toBeInTheDocument()
    })
  })

  it('only marks the selected slot when slots share the same start time', async () => {
    const user = userEvent.setup()
    const sharedStart = isoDaysFromNow(3, 11)
    const slots: ScheduleSlot[] = [
      {
        id: 'slot-1',
        startAt: sharedStart,
        endAt: minutesAfter(sharedStart, 45),
        available: true,
      },
      {
        id: 'slot-2',
        startAt: sharedStart,
        endAt: minutesAfter(sharedStart, 30),
        available: true,
      },
    ]

    render(<BookingCalendarHarness slots={slots} />)

    const longerSlot = await screen.findByRole('button', { name: /11:00 - 11:45/ })
    const shorterSlot = await screen.findByRole('button', { name: /11:00 - 11:30/ })

    await user.click(longerSlot)

    expect(longerSlot).toHaveClass('active')
    expect(shorterSlot).not.toHaveClass('active')
  })

  it('clears the selected slot after switching days', async () => {
    const user = userEvent.setup()
    render(<BookingCalendarHarness slots={availableSlots} />)

    const firstDaySlot = await screen.findByRole('button', { name: /09:00 - 10:00/ })
    await user.click(firstDaySlot)

    expect(firstDaySlot).toHaveClass('active')

    const dayButtons = await screen.findAllByRole('button', { name: /1 slots/ })
    await user.click(dayButtons[1])
    await user.click(dayButtons[0])

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /09:00 - 10:00/ }),
      ).not.toHaveClass('active')
    })
  })
})
