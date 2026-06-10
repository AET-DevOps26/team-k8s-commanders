import type { AuthSession } from '../clientApi'

export type Route =
  | '/'
  | '/login'
  | '/register'
  | '/patient'
  | '/patient/profile'
  | '/patient/book'
  | '/doctor'

export type NavigateHandler = (path: Route) => void

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

export type PatientProfileProps = PatientDashboardProps & {
  onSessionUpdated: (session: AuthSession) => void
}

export type DoctorDashboardProps = {
  session: AuthSession
  onLogout: () => void
  onNavigate: NavigateHandler
}
