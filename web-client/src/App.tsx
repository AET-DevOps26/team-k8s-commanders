import { useEffect, useState } from 'react'
import './App.css'
import { AdminDashboard } from './admin'
import type { AuthSession } from './clientApi'
import { logout, setSessionExpiredHandler } from './clientApi'
import { AuthForm } from './components/auth/AuthForm'
import { DoctorBookAppointmentPage } from './components/doctor/DoctorBookAppointmentPage'
import { DoctorDashboard } from './components/doctor/DoctorDashboard'
import { DoctorPatientsPage } from './components/doctor/DoctorPatientsPage'
import { DoctorSchedulePage } from './components/doctor/DoctorSchedulePage'
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
  const [patientBookingSuccess, setPatientBookingSuccess] = useState(false)
  const [doctorBookingSuccess, setDoctorBookingSuccess] = useState(false)
  const [sessionExpired, setSessionExpired] = useState(false)

  useEffect(() => {
    function handlePopState() {
      setRoute(getInitialRoute())
    }

    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [])

  useEffect(() => {
    // Any authenticated request can 401 at any time — most commonly a
    // redeploy rotating JWT_SECRET and invalidating every token issued
    // before it. Without this, the UI stays on a stale dashboard showing a
    // "logged in" state the backend no longer honors.
    setSessionExpiredHandler(() => {
      clearSession()
      setSession(null)
      setSessionExpired(true)
      navigate('/login')
    })

    return () => setSessionExpiredHandler(null)
  }, [])

  function navigate(path: Route) {
    window.history.pushState({}, '', path)
    setRoute(path)
  }

  function handleAuthenticated(nextSession: AuthSession) {
    saveSession(nextSession)
    setSession(nextSession)
    setSessionExpired(false)
    navigate(
      nextSession.user.role === 'ADMIN'
        ? '/admin'
        : nextSession.user.role === 'DOCTOR'
          ? '/doctor'
          : '/patient',
    )
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
        notice={route === '/login' && sessionExpired ? 'Your session has expired. Please log in again.' : undefined}
        onAuthenticated={handleAuthenticated}
        onNavigate={navigate}
      />
    )
  }

  if (route === '/admin') {
    if (!session) {
      return (
        <AuthForm
          mode="login"
          onAuthenticated={handleAuthenticated}
          onNavigate={navigate}
        />
      )
    }

    return (
      <AdminDashboard
        session={session}
        onLogout={handleLogout}
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

    if (session.user.role !== 'PATIENT') {
      if (session.user.role === 'DOCTOR') {
        return (
          <DoctorDashboard
            session={session}
            onLogout={handleLogout}
            onNavigate={navigate}
          />
        )
      }

      return (
        <AdminDashboard
          session={session}
          onLogout={handleLogout}
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
          onBooked={() => {
            setPatientBookingSuccess(true)
            navigate('/patient')
          }}
        />
      )
    }

    return (
      <PatientDashboard
        session={session}
        onLogout={handleLogout}
        onNavigate={navigate}
        bookingSuccess={patientBookingSuccess}
        onBookingSuccessAcknowledged={() => setPatientBookingSuccess(false)}
      />
    )
  }

  if (route === '/doctor' || route === '/doctor/schedule' || route === '/doctor/patients' || route === '/doctor/book') {
    if (!session) {
      return (
        <AuthForm
          mode="login"
          onAuthenticated={handleAuthenticated}
          onNavigate={navigate}
        />
      )
    }

    if (route === '/doctor/schedule') {
      return (
        <DoctorSchedulePage
          session={session}
          onLogout={handleLogout}
          onNavigate={navigate}
        />
      )
    }

    if (route === '/doctor/book') {
      return (
        <DoctorBookAppointmentPage
          session={session}
          onLogout={handleLogout}
          onNavigate={navigate}
          onBooked={() => {
            setDoctorBookingSuccess(true)
            navigate('/doctor')
          }}
        />
      )
    }

    if (route === '/doctor/patients') {
      return (
        <DoctorPatientsPage
          session={session}
          onLogout={handleLogout}
          onNavigate={navigate}
        />
      )
    }

    return (
      <DoctorDashboard
        session={session}
        onLogout={handleLogout}
        onNavigate={navigate}
        bookingSuccess={doctorBookingSuccess}
        onBookingSuccessAcknowledged={() => setDoctorBookingSuccess(false)}
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
