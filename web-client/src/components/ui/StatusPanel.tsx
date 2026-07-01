import type { ReactNode } from 'react'

type StatusPanelProps = {
  children?: ReactNode
  title: string
  text?: string
}

export function StatusPanel({ children, title, text }: StatusPanelProps) {
  return (
    <section className="status-panel">
      <strong>{title}</strong>
      {text && <p>{text}</p>}
      {children && <div className="status-panel-actions">{children}</div>}
    </section>
  )
}
