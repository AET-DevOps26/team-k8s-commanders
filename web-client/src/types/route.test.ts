import { describe, expect, it } from 'vitest'
import { dashboardLabel, dashboardPath } from './route'

describe('dashboardPath', () => {
  it('routes each role to its own dashboard', () => {
    expect(dashboardPath('ADMIN')).toBe('/admin')
    expect(dashboardPath('DOCTOR')).toBe('/doctor')
    expect(dashboardPath('PATIENT')).toBe('/patient')
  })

  it('defaults unknown roles to the patient dashboard', () => {
    expect(dashboardPath('SOMETHING_ELSE')).toBe('/patient')
  })
})

describe('dashboardLabel', () => {
  it('labels each role area', () => {
    expect(dashboardLabel('ADMIN')).toBe('Admin area')
    expect(dashboardLabel('DOCTOR')).toBe('Doctor area')
    expect(dashboardLabel('PATIENT')).toBe('Patient area')
  })
})
