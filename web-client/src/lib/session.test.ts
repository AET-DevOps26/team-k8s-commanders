import { describe, expect, it } from 'vitest'
import { SESSION_KEY } from '../constants/session'
import { patientSession, seedStoredSession } from '../test/fixtures'
import { clearSession, getInitialRoute, getStoredSession, saveSession } from './session'

describe('getInitialRoute', () => {
  it.each([
    '/login',
    '/register',
    '/patient',
    '/patient/profile',
    '/patient/book',
    '/admin',
    '/doctor',
    '/doctor/schedule',
    '/doctor/patients',
    '/doctor/book',
  ] as const)('returns %s for a known app path', (path) => {
    window.history.replaceState({}, '', path)
    expect(getInitialRoute()).toBe(path)
  })

  it('falls back to the landing page for unknown paths', () => {
    window.history.replaceState({}, '', '/unknown/deep/path')
    expect(getInitialRoute()).toBe('/')
  })
})

describe('stored session', () => {
  it('round-trips a saved session', () => {
    saveSession(patientSession)
    expect(getStoredSession()).toEqual(patientSession)
  })

  it('returns null when nothing is stored', () => {
    expect(getStoredSession()).toBeNull()
  })

  it('discards corrupted JSON and cleans up storage', () => {
    window.localStorage.setItem(SESSION_KEY, '{not json')
    expect(getStoredSession()).toBeNull()
    expect(window.localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('discards sessions without an access token', () => {
    window.localStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ user: patientSession.user }),
    )
    expect(getStoredSession()).toBeNull()
    expect(window.localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('discards sessions without a user id', () => {
    window.localStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ accessToken: 'token', user: {} }),
    )
    expect(getStoredSession()).toBeNull()
  })

  it('clearSession removes the stored session', () => {
    seedStoredSession()
    clearSession()
    expect(getStoredSession()).toBeNull()
  })
})
