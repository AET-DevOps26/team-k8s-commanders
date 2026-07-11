import { useEffect, useState } from 'react'
import type { ScheduleSlot } from '../../clientApi'
import { getDoctorSchedule } from '../../clientApi'
import { userMessage } from '../../lib/messages'
import type { DoctorDashboardProps } from '../../types/route'
import { DoctorSubNav } from '../layout/DoctorSubNav'
import { ShellNav } from '../layout/ShellNav'
import { StatusPanel } from '../ui/StatusPanel'
import { AvailabilitySlotCreator } from './AvailabilitySlotCreator'
import { AvailabilitySlotsPanel } from './AvailabilitySlotsPanel'
import { DoctorAiFloatingAssistant } from './DoctorAiFloatingAssistant'

export function DoctorSchedulePage({
  session,
  onLogout,
  onNavigate,
}: DoctorDashboardProps) {
  const [slots, setSlots] = useState<ScheduleSlot[]>([])
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const doctorId = session.user.id
  const token = session.accessToken

  useEffect(() => {
    let isActive = true

    async function loadSchedule() {
      setLoading(true)
      setError('')

      try {
        const schedule = await getDoctorSchedule(doctorId, token)

        if (isActive) {
          setSlots(schedule.slots)
        }
      } catch {
        if (isActive) {
          setError(userMessage('Availability could not be loaded. Please try again.'))
          setSlots([])
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadSchedule()

    return () => {
      isActive = false
    }
  }, [doctorId, token])

  if (session.user.role !== 'DOCTOR') {
    return (
      <main className="landing-page app-page">
        <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
        <section className="empty-state">
          <p className="eyebrow">Doctor schedule</p>
          <h1>Doctor account required.</h1>
          <button className="primary-button" onClick={onLogout} type="button">
            Logout
          </button>
        </section>
      </main>
    )
  }

  function handleCreated(slot: ScheduleSlot) {
    setSlots((current) => [...current, slot])
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell doctor-dashboard-shell">
        <DoctorSubNav active="schedule" onNavigate={onNavigate} />
        <header className="patient-hero doctor-hero">
          <div>
            <p className="eyebrow">Doctor schedule</p>
            <h1>Availability</h1>
            <p>Publish bookable time slots. Patients are assigned only when they book.</p>
          </div>
        </header>

        {isLoading && <StatusPanel title="Loading availability" />}
        {error && <StatusPanel title="Schedule API unavailable" text={error} />}

        {!isLoading && !error && (
          <section className="schedule-management-grid">
            <AvailabilitySlotCreator
              doctorId={doctorId}
              token={token}
              onCreated={handleCreated}
            />
            <AvailabilitySlotsPanel slots={slots} />
          </section>
        )}
      </section>
      <DoctorAiFloatingAssistant
        contextKey="doctor:general-medical"
        inputLabel="Ask a medical question"
        placeholder="Ask a general medical question or clarify clinical guidance."
        title="Medical assistant"
        token={token}
      />
    </main>
  )
}
