import { useEffect, useState } from 'react'
import './App.css'
import type { AuthSession } from './clientApi'
import { logout } from './clientApi'
import { AuthForm } from './components/auth/AuthForm'
import { LandingPage } from './components/landing/LandingPage'
import { PatientBookingPage } from './components/patient/PatientBookingPage'
import { PatientDashboard } from './components/patient/PatientDashboard'
import { PatientProfilePage } from './components/patient/PatientProfilePage'
import {
  clearSession,
  getInitialRoute,
  getStoredSession,
  saveSession,
} from './lib/session'
import type { Route } from './types/route'

export default function App() {
  const [route, setRoute] = useState<Route>(getInitialRoute)
  const [session, setSession] = useState<AuthSession | null>(getStoredSession)

  useEffect(() => {
    function handlePopState() {
      setRoute(getInitialRoute())
    }

    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [])

  function navigate(path: Route) {
    window.history.pushState({}, '', path)
    setRoute(path)
  }

  function handleAuthenticated(nextSession: AuthSession) {
    saveSession(nextSession)
    setSession(nextSession)
    navigate('/patient')
  }

  function handleSessionUpdated(nextSession: AuthSession) {
    setSession(nextSession)
  }

  async function handleLogout() {
    if (session) {
      logout(session.accessToken).catch(() => {
        // Local sign-out must still work if backend session endpoint is unavailable.
      })
    }

    clearSession()
    setSession(null)
    navigate('/')
  }

  if (route === '/login' || route === '/register') {
    return (
      <AuthForm
        mode={route === '/register' ? 'register' : 'login'}
        onAuthenticated={handleAuthenticated}
        onNavigate={navigate}
      />
    )
  }

  if (route === '/patient' || route === '/patient/profile' || route === '/patient/book') {
    if (!session) {
      return (
        <AuthForm
          mode="login"
          onAuthenticated={handleAuthenticated}
          onNavigate={navigate}
        />
      )
    }

    if (route === '/patient/profile') {
      return (
        <PatientProfilePage
          session={session}
          onLogout={handleLogout}
          onNavigate={navigate}
          onSessionUpdated={handleSessionUpdated}
        />
      )
    }

    if (route === '/patient/book') {
      return (
        <PatientBookingPage
          session={session}
          onLogout={handleLogout}
          onNavigate={navigate}
        />
      )
    }

    return (
      <PatientDashboard
        session={session}
        onLogout={handleLogout}
        onNavigate={navigate}
      />
    )
  }

  return (
    <LandingPage
      session={session}
      onNavigate={navigate}
      onLogout={handleLogout}
    />
  )
}
