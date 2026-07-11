import type { FormEvent, ReactNode } from 'react'

type SelectOption = {
  value: string
  label: string
}

type BookingSearchSectionProps = {
  title: string
  searchLabel?: string
  searchPlaceholder: string
  searchValue: string
  onSearchChange: (value: string) => void
  searchOptions?: SelectOption[]
  searchDisabled?: boolean
  secondaryLabel?: string
  secondaryPlaceholder?: string
  secondaryValue?: string
  onSecondaryChange?: (value: string) => void
  secondaryOptions?: SelectOption[]
  secondaryDisabled?: boolean
  onSubmit: (event?: FormEvent<HTMLFormElement>) => void
  isLoading: boolean
  submitLabel?: string
  loadingLabel?: string
  results?: ReactNode
  scrollableResults?: boolean
  showSubmit?: boolean
  helperText?: string
  layout?: 'split' | 'inline'
}

export function BookingSearchSection({
  title,
  searchLabel = 'Search',
  searchPlaceholder,
  searchValue,
  onSearchChange,
  searchOptions,
  searchDisabled = false,
  secondaryLabel,
  secondaryPlaceholder,
  secondaryValue,
  onSecondaryChange,
  secondaryOptions,
  secondaryDisabled = false,
  onSubmit,
  isLoading,
  submitLabel,
  loadingLabel = 'Loading',
  results,
  scrollableResults = false,
  showSubmit = true,
  helperText,
  layout = 'split',
}: BookingSearchSectionProps) {
  return (
    <section className={`booking-grid booking-grid-${layout}`}>
      <form className="auth-card booking-search" onSubmit={onSubmit}>
        <h2>{title}</h2>
        <div className="booking-search-fields">
          <label>
            {searchLabel}
            {searchOptions ? (
              <select
                disabled={searchDisabled}
                onChange={(event) => onSearchChange(event.target.value)}
                value={searchValue}
              >
                <option value="">{searchPlaceholder}</option>
                {searchOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            ) : (
              <input
                disabled={searchDisabled}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder={searchPlaceholder}
                type="search"
                value={searchValue}
              />
            )}
          </label>
          {secondaryLabel && onSecondaryChange && (
            <label>
              {secondaryLabel}
              {secondaryOptions ? (
                <select
                  disabled={secondaryDisabled}
                  onChange={(event) => onSecondaryChange(event.target.value)}
                  value={secondaryValue ?? ''}
                >
                  <option value="">{secondaryPlaceholder ?? 'Select'}</option>
                  {secondaryOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  disabled={secondaryDisabled}
                  onChange={(event) => onSecondaryChange(event.target.value)}
                  placeholder={secondaryPlaceholder}
                  type="text"
                  value={secondaryValue ?? ''}
                />
              )}
            </label>
          )}
          {showSubmit && (
            <button className="primary-button" disabled={isLoading} type="submit">
              {isLoading ? loadingLabel : submitLabel}
            </button>
          )}
        </div>
        {helperText && <p className="booking-search-helper">{helperText}</p>}
      </form>

      {results && (
        <div className={`doctor-results${scrollableResults ? ' booking-results-scroll' : ''}`}>
          {results}
        </div>
      )}
    </section>
  )
}
