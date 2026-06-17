import type { NavigateHandler, Route } from '../../types/route'

type DoctorSubNavProps = {
  active: 'dashboard' | 'schedule' | 'patients'
  onNavigate: NavigateHandler
}

export function DoctorSubNav({ active, onNavigate }: DoctorSubNavProps) {
  const items: Array<{ key: typeof active; label: string; route: Route }> = [
    { key: 'dashboard', label: 'Overview', route: '/doctor' },
    { key: 'schedule', label: 'Availability', route: '/doctor/schedule' },
    { key: 'patients', label: 'Patient records', route: '/doctor/patients' },
  ]

  return (
    <div className="patient-tabs" role="navigation" aria-label="Doctor navigation">
      {items.map((item) => (
        <button
          className={active === item.key ? 'active' : ''}
          key={item.key}
          onClick={() => onNavigate(item.route)}
          type="button"
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
