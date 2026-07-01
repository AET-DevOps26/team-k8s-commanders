import type { UserProfile } from '../../clientApi'
import { EmptyPanel } from '../ui/EmptyPanel'

type BookingEntityResultsProps = {
  entities: UserProfile[]
  selectedEntityId: string | null
  onSelectEntity: (entity: UserProfile) => void
  emptyMessage: string
  subtitle: (entity: UserProfile) => string
}

export function BookingEntityResults({
  entities,
  selectedEntityId,
  onSelectEntity,
  emptyMessage,
  subtitle,
}: BookingEntityResultsProps) {
  if (!entities.length) {
    return <EmptyPanel text={emptyMessage} />
  }

  return (
    <>
      {entities.map((entity) => (
        <button
          className={selectedEntityId === entity.id ? 'doctor-card active' : 'doctor-card'}
          key={entity.id}
          onClick={() => onSelectEntity(entity)}
          type="button"
        >
          <strong>{entity.name}</strong>
          <span>{subtitle(entity)}</span>
          <small>{entity.email}</small>
        </button>
      ))}
    </>
  )
}
