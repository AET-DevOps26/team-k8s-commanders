import type { NavigateHandler, Route } from '../../types/route'

type PatientSubNavProps = {
  active: 'dashboard' | 'profile' | 'book'
  onNavigate: NavigateHandler
}

export function PatientSubNav({ active, onNavigate }: PatientSubNavProps) {
  const items: Array<{ key: typeof active; label: string; route: Route }> = [
    { key: 'dashboard', label: 'Dashboard', route: '/patient' },
    { key: 'book', label: 'Book appointment', route: '/patient/book' },
    { key: 'profile', label: 'Profile', route: '/patient/profile' },
  ]

  return (
    <div className="patient-tabs" role="navigation" aria-label="Patient navigation">
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
