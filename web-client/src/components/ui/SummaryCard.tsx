type SummaryCardProps = {
  label: string
  value: string
  text: string
}

export function SummaryCard({ label, value, text }: SummaryCardProps) {
  return (
    <article className="summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{text}</p>
    </article>
  )
}
