import type { AuthSession, UserProfile } from '../clientApi'
import type { Route } from '../routing'

export type AdminDashboardProps = {
  session: AuthSession
  onLogout: () => void
  onNavigate: (path: Route) => void
}

export type AdminData = {
  users: UserProfile[]
  total: number
  page: number
  totalPages: number
}
