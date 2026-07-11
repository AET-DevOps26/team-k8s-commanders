import type { AuthSession } from '../../clientApi'
import type { NavigateHandler } from '../../types/route'
import { AppLink } from '../ui/AppLink'

type ShellNavProps = {
  session: AuthSession | null
  onNavigate: NavigateHandler
  onLogout?: () => void
}

export function ShellNav({ session, onNavigate, onLogout }: ShellNavProps) {
  return (
    <nav className="site-nav" aria-label="Primary navigation">
      <AppLink className="brand" to="/" onNavigate={onNavigate}>
        <img className="brand-mark" src="/favicon.svg" alt="" aria-hidden="true" />
        <span>CareDesk</span>
      </AppLink>
      <div className="nav-links">
        <a href="/#features">Features</a>
        {session ? (
          <>
            {session.user.role === 'ADMIN' ? (
              <AppLink to="/admin" onNavigate={onNavigate}>
                Admin dashboard
              </AppLink>
            ) : session.user.role === 'DOCTOR' ? (
              <AppLink to="/doctor" onNavigate={onNavigate}>
                Doctor dashboard
              </AppLink>
            ) : (
              <AppLink to="/patient" onNavigate={onNavigate}>
                Patient dashboard
              </AppLink>
            )}
            <button className="link-button" type="button" onClick={onLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <AppLink to="/login" onNavigate={onNavigate}>
              Login
            </AppLink>
            <AppLink className="nav-cta" to="/register" onNavigate={onNavigate}>
              Sign up
            </AppLink>
          </>
        )}
      </div>
    </nav>
  )
}
