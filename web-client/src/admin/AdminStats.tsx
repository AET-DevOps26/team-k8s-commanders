import type { UserStats } from '../clientApi'
import { SummaryCard } from '../components/ui/SummaryCard'

type AdminStatsProps = {
  stats: UserStats
}

export function AdminStats({ stats }: AdminStatsProps) {
  return (
    <section className="patient-summary admin-stats">
      <SummaryCard label="Total" value={String(stats.total)} text="All accounts" />
      <SummaryCard
        label="Patients"
        value={String(stats.patients)}
        text="Role: PATIENT"
      />
      <SummaryCard
        label="Doctors"
        value={String(stats.doctors)}
        text="Role: DOCTOR"
      />
      <SummaryCard label="Admins" value={String(stats.admins)} text="Role: ADMIN" />
      <SummaryCard label="Active" value={String(stats.active)} text="Can sign in" />
      <SummaryCard
        label="Disabled"
        value={String(stats.disabled)}
        text="Deactivated"
      />
    </section>
  )
}
