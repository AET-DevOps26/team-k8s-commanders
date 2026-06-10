type FeatureIconProps = {
  type: string
}

export function FeatureIcon({ type }: FeatureIconProps) {
  if (type === 'calendar') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M7 3v4M17 3v4M4 9h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" />
        <path d="M8 13h3M13 13h3M8 16h3" />
      </svg>
    )
  }

  if (type === 'records') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M8 4h8l3 3v13H5V4h3Z" />
        <path d="M15 4v4h4M8 12h8M8 16h5M10.5 8h3" />
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 3l1.7 5.1L19 10l-5.3 1.9L12 17l-1.7-5.1L5 10l5.3-1.9L12 3Z" />
      <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15ZM5 14l.6 1.6L7 16l-1.4.4L5 18l-.6-1.6L3 16l1.4-.4L5 14Z" />
    </svg>
  )
}
