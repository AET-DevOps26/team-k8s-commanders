import type { AuthSession } from '../clientApi'

export type Route =
  | '/'
  | '/login'
  | '/register'
  | '/patient'
  | '/patient/profile'
  | '/patient/book'
  | '/admin'
  | '/doctor'
  | '/doctor/schedule'
  | '/doctor/patients'
  | '/doctor/book'

export type NavigateHandler = (path: Route) => void

export function dashboardPath(role: string): Route {
  if (role === 'ADMIN') {
    return '/admin'
  }
  if (role === 'DOCTOR') {
    return '/doctor'
  }
  return '/patient'
}

export function dashboardLabel(role: string): string {
  if (role === 'ADMIN') {
    return 'Admin area'
  }
  if (role === 'DOCTOR') {
    return 'Doctor area'
  }
  return 'Patient area'
}

export type AuthFormProps = {
  mode: 'login' | 'register'
  onAuthenticated: (session: AuthSession) => void
  onNavigate: NavigateHandler
}

export type PatientDashboardProps = {
  session: AuthSession
  onLogout: () => void
  onNavigate: NavigateHandler
}

export type PatientBookingPageProps = PatientDashboardProps & {
  onBooked: () => void
}

export type PatientDashboardViewProps = PatientDashboardProps & {
  bookingSuccess?: boolean
  onBookingSuccessAcknowledged?: () => void
}

export type PatientProfileProps = PatientDashboardProps & {
  onSessionUpdated: (session: AuthSession) => void
}

export type DoctorDashboardProps = {
  session: AuthSession
  onLogout: () => void
  onNavigate: NavigateHandler
}

export type DoctorDashboardViewProps = DoctorDashboardProps & {
  bookingSuccess?: boolean
  onBookingSuccessAcknowledged?: () => void
}
