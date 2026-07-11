import { describe, expect, it } from 'vitest'
import type { ScheduleSlot } from '../clientApi'
import {
  formatAppointmentDate,
  formatTimeRange,
  isPastDateTime,
  slotDuration,
} from './dates'

const slot: ScheduleSlot = {
  startAt: '2026-07-01T09:00:00.000Z',
  endAt: '2026-07-01T09:45:00.000Z',
  available: true,
}

describe('formatAppointmentDate', () => {
  it('formats an ISO timestamp as a German medium date with time', () => {
    const formatted = formatAppointmentDate('2026-07-01T09:00:00.000Z')
    expect(formatted).toMatch(/^01\.07\.2026, \d{2}:\d{2}$/)
  })
})

describe('isPastDateTime', () => {
  const now = new Date('2026-07-01T12:00:00.000Z').getTime()

  it('flags earlier timestamps as past', () => {
    expect(isPastDateTime('2026-07-01T11:59:59.000Z', now)).toBe(true)
  })

  it('keeps future timestamps', () => {
    expect(isPastDateTime('2026-07-01T12:00:01.000Z', now)).toBe(false)
  })
})

describe('formatTimeRange', () => {
  it('joins start and end time with a dash', () => {
    expect(formatTimeRange(slot)).toMatch(/^\d{2}:\d{2} - \d{2}:\d{2}$/)
  })
})

describe('slotDuration', () => {
  it('returns the duration in minutes', () => {
    expect(slotDuration(slot)).toBe(45)
  })
})
