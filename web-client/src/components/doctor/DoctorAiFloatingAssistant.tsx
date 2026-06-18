import { useEffect, useState } from 'react'
import { DoctorAiAssistant } from './DoctorAiAssistant'

type DoctorAiFloatingAssistantProps = {
  appointmentId?: string | null
  contextKey: string
  inputLabel?: string
  patientId?: string | null
  patientName?: string
  placeholder?: string
  prompts?: string[]
  title: string
  token: string
}

export function DoctorAiFloatingAssistant({
  appointmentId = null,
  contextKey,
  inputLabel,
  patientId = null,
  patientName,
  placeholder,
  prompts = [],
  title,
  token,
}: DoctorAiFloatingAssistantProps) {
  const [isOpen, setOpen] = useState(false)
  const [hasOpened, setHasOpened] = useState(false)

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen])

  return (
    <>
      <button
        aria-label="Open doctor AI assistant"
        className="doctor-ai-fab"
        onClick={() => {
          setHasOpened(true)
          setOpen(true)
        }}
        type="button"
      >
        <span>AI</span>
      </button>

      {hasOpened && (
        <div
          aria-label="Doctor AI assistant"
          aria-hidden={!isOpen}
          aria-modal={isOpen}
          className={
            isOpen
              ? 'doctor-ai-modal-backdrop'
              : 'doctor-ai-modal-backdrop is-hidden'
          }
          role={isOpen ? 'dialog' : undefined}
        >
          <div className="doctor-ai-modal">
            <div className="doctor-ai-modal-head">
              <div>
                <p className="eyebrow">Doctor AI</p>
                <h2>{title}</h2>
              </div>
              <button
                aria-label="Close doctor AI assistant"
                className="secondary-button compact-button"
                onClick={() => setOpen(false)}
                type="button"
              >
                Close
              </button>
            </div>
            <DoctorAiAssistant
              appointmentId={appointmentId}
              contextKey={contextKey}
              inputLabel={inputLabel}
              patientId={patientId}
              patientName={patientName}
              placeholder={placeholder}
              prompts={prompts}
              title={title}
              token={token}
            />
          </div>
        </div>
      )}
    </>
  )
}
