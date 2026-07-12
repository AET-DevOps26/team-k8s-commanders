import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type { ScheduleSlot } from '../../clientApi'
import { availableSlots } from '../../test/fixtures'
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
})
