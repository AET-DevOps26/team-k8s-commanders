type EmptyPanelProps = {
  text: string
}

export function EmptyPanel({ text }: EmptyPanelProps) {
  return <p className="empty-panel">{text}</p>
}
