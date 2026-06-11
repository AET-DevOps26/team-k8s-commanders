import { ReactNode } from 'react'
import type { Route } from '../routing'

type AppLinkProps = {
  to: Route
  className?: string
  children: ReactNode
  onNavigate: (path: Route) => void
}

export function AppLink({
  to,
  className,
  children,
  onNavigate,
}: AppLinkProps) {
  return (
    <a
      className={className}
      href={to}
      onClick={(event) => {
        event.preventDefault()
        onNavigate(to)
      }}
    >
      {children}
    </a>
  )
}
