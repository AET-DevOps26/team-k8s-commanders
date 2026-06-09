import type { AuthSession } from '../clientApi'
import { SESSION_KEY } from '../constants/session'
import type { Route } from '../types/route'

export function getInitialRoute(): Route {
  const path = window.location.pathname

  if (
    path === '/login' ||
    path === '/register' ||
    path === '/patient' ||
    path === '/patient/profile' ||
    path === '/patient/book' ||
    path === '/admin'
  ) {
    return path
  }

  return '/'
}

export function getStoredSession() {
  const rawSession = window.localStorage.getItem(SESSION_KEY)

  if (!rawSession) {
    return null
  }

  try {
    const session = JSON.parse(rawSession) as AuthSession

    if (session.accessToken && session.user?.id) {
      return session
    }

    window.localStorage.removeItem(SESSION_KEY)
  } catch {
    window.localStorage.removeItem(SESSION_KEY)
  }

  return null
}

export function saveSession(session: AuthSession) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_KEY)
}
