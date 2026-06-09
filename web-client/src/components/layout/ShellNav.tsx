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
        <span className="brand-mark">+</span>
        <span>CareDesk</span>
      </AppLink>
      <div className="nav-links">
        <a href="/#features">Features</a>
        {session ? (
          <>
            <AppLink to="/patient" onNavigate={onNavigate}>
              Patient dashboard
            </AppLink>
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
