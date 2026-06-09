import type { AuthSession } from '../clientApi'

export type Route =
  | '/'
  | '/login'
  | '/register'
  | '/patient'
  | '/patient/profile'
  | '/patient/book'
  | '/admin'

export type NavigateHandler = (path: Route) => void

export function dashboardPath(role: string): Route {
  return role === 'ADMIN' ? '/admin' : '/patient'
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

export type PatientProfileProps = PatientDashboardProps & {
  onSessionUpdated: (session: AuthSession) => void
}
