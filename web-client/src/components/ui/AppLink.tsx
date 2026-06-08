import type { ReactNode } from 'react'
import type { NavigateHandler, Route } from '../../types/route'

type AppLinkProps = {
  to: Route
  className?: string
  children: ReactNode
  onNavigate: NavigateHandler
}

export function AppLink({ to, className, children, onNavigate }: AppLinkProps) {
  return (
    <a
      className={className}
      href={to}
      onClick={(event) => {
        const isPlainLeftClick =
          event.button === 0 &&
          !event.metaKey &&
          !event.ctrlKey &&
          !event.shiftKey &&
          !event.altKey

        if (!isPlainLeftClick) {
          return
        }

        event.preventDefault()
        onNavigate(to)
      }}
    >
      {children}
    </a>
  )
}
