type StatusPanelProps = {
  title: string
  text?: string
}

export function StatusPanel({ title, text }: StatusPanelProps) {
  return (
    <section className="status-panel">
      <strong>{title}</strong>
      {text && <p>{text}</p>}
    </section>
  )
}
