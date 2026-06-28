import type { FormEvent, ReactNode } from 'react'

type BookingSearchSectionProps = {
  title: string
  searchLabel?: string
  searchPlaceholder: string
  searchValue: string
  onSearchChange: (value: string) => void
  secondaryLabel?: string
  secondaryPlaceholder?: string
  secondaryValue?: string
  onSecondaryChange?: (value: string) => void
  onSubmit: (event?: FormEvent<HTMLFormElement>) => void
  isLoading: boolean
  submitLabel: string
  loadingLabel: string
  results: ReactNode
  scrollableResults?: boolean
}

export function BookingSearchSection({
  title,
  searchLabel = 'Search',
  searchPlaceholder,
  searchValue,
  onSearchChange,
  secondaryLabel,
  secondaryPlaceholder,
  secondaryValue,
  onSecondaryChange,
  onSubmit,
  isLoading,
  submitLabel,
  loadingLabel,
  results,
  scrollableResults = false,
}: BookingSearchSectionProps) {
  return (
    <section className="booking-grid">
      <form className="auth-card booking-search" onSubmit={onSubmit}>
        <h2>{title}</h2>
        <label>
          {searchLabel}
          <input
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder={searchPlaceholder}
            type="search"
            value={searchValue}
          />
        </label>
        {secondaryLabel && onSecondaryChange && (
          <label>
            {secondaryLabel}
            <input
              onChange={(event) => onSecondaryChange(event.target.value)}
              placeholder={secondaryPlaceholder}
              type="text"
              value={secondaryValue ?? ''}
            />
          </label>
        )}
        <button className="primary-button" disabled={isLoading} type="submit">
          {isLoading ? loadingLabel : submitLabel}
        </button>
      </form>

      <div className={`doctor-results${scrollableResults ? ' booking-results-scroll' : ''}`}>
        {results}
      </div>
    </section>
  )
}
