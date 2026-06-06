import { FormEvent, useEffect, useState } from 'react'
import type { ScheduleSlot, UserProfile } from '../../clientApi'
import {
  bookAppointment,
  getDoctorSchedule,
  listDoctors,
} from '../../clientApi'
import { formatAppointmentDate, formatTimeRange } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import type { PatientDashboardProps } from '../../types/route'
import { PatientSubNav } from '../layout/PatientSubNav'
import { ShellNav } from '../layout/ShellNav'
import { EmptyPanel } from '../ui/EmptyPanel'
import { StatusPanel } from '../ui/StatusPanel'

export function PatientBookingPage({
  session,
  onLogout,
  onNavigate,
}: PatientDashboardProps) {
  const [query, setQuery] = useState('')
  const [specialization, setSpecialization] = useState('')
  const [doctors, setDoctors] = useState<UserProfile[]>([])
  const [selectedDoctor, setSelectedDoctor] = useState<UserProfile | null>(null)
  const [slots, setSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [reason, setReason] = useState('')
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(false)

  async function searchDoctors(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    setLoading(true)
    setError('')
    setStatus('')

    try {
      const response = await listDoctors(session.accessToken, {
        q: query,
        specialization,
        size: 12,
      })
      setDoctors(response.content)
      if (!response.content.some((doctor) => doctor.id === selectedDoctor?.id)) {
        setSelectedDoctor(null)
        setSlots([])
        setSelectedSlot(null)
      }
    } catch {
      setError(userMessage('Doctors could not be loaded. Please try again in a moment.'))
    } finally {
      setLoading(false)
    }
  }

  async function selectDoctor(doctor: UserProfile) {
    setSelectedDoctor(doctor)
    setSelectedSlot(null)
    setError('')
    setStatus('')

    try {
      const schedule = await getDoctorSchedule(doctor.id, session.accessToken)
      setSlots(schedule.slots)
    } catch {
      setSlots([])
      setError(userMessage('Available times could not be loaded. Please choose another doctor or try again.'))
    }
  }

  async function handleBooking() {
    if (!selectedDoctor || !selectedSlot) {
      setError('Please select a doctor and time slot')
      return
    }

    setError('')
    setStatus('')

    try {
      const duration = Math.round(
        (new Date(selectedSlot.endAt).getTime() - new Date(selectedSlot.startAt).getTime()) / 60000,
      )
      await bookAppointment(session.accessToken, {
        patientId: session.user.id,
        doctorId: selectedDoctor.id,
        dateTime: selectedSlot.startAt,
        duration,
        reason: reason || undefined,
      })
      setStatus('Appointment booked')
      setReason('')
      await selectDoctor(selectedDoctor)
    } catch {
      setError(userMessage('This appointment could not be booked. Please choose another time or try again.'))
    }
  }

  useEffect(() => {
    searchDoctors()
    // Initial doctor list should load once for the active session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session.accessToken])

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <PatientSubNav active="book" onNavigate={onNavigate} />
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Booking</p>
            <h1>Find a doctor</h1>
            <p>Search CareDesk doctors, choose an available slot, and book directly.</p>
          </div>
        </header>

        {error && <StatusPanel title="We could not complete your booking" text={error} />}
        {status && <StatusPanel title={status} text="Your appointment is now listed in your dashboard." />}

        <section className="booking-grid">
          <form className="auth-card booking-search" onSubmit={searchDoctors}>
            <h2>Doctor search</h2>
            <label>
              Search
              <input
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Name or specialization"
                type="search"
                value={query}
              />
            </label>
            <label>
              Specialization
              <input
                onChange={(event) => setSpecialization(event.target.value)}
                placeholder="General Medicine"
                type="text"
                value={specialization}
              />
            </label>
            <button className="primary-button" disabled={isLoading} type="submit">
              {isLoading ? 'Searching' : 'Search doctors'}
            </button>
          </form>

          <div className="doctor-results">
            {doctors.length ? (
              doctors.map((doctor) => (
                <button
                  className={selectedDoctor?.id === doctor.id ? 'doctor-card active' : 'doctor-card'}
                  key={doctor.id}
                  onClick={() => selectDoctor(doctor)}
                  type="button"
                >
                  <strong>{doctor.name}</strong>
                  <span>{doctor.specialization ?? 'CareDesk doctor'}</span>
                  <small>{doctor.email}</small>
                </button>
              ))
            ) : (
              <EmptyPanel text="No doctors found. Try another search." />
            )}
          </div>
        </section>

        {selectedDoctor && (
          <section className="calendar-panel dashboard-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Calendar</p>
                <h2>{selectedDoctor.name}</h2>
              </div>
            </div>
            <div className="slot-grid">
              {slots.length ? (
                slots.map((slot) => (
                  <button
                    className={selectedSlot?.startAt === slot.startAt ? 'slot-button active' : 'slot-button'}
                    key={`${slot.startAt}-${slot.endAt}`}
                    onClick={() => setSelectedSlot(slot)}
                    type="button"
                  >
                    <span>{formatAppointmentDate(slot.startAt)}</span>
                    <strong>{formatTimeRange(slot)}</strong>
                  </button>
                ))
              ) : (
                <EmptyPanel text="No available times right now." />
              )}
            </div>
            <label className="reason-field">
              Reason
              <textarea
                onChange={(event) => setReason(event.target.value)}
                placeholder="Short reason for the visit"
                rows={3}
                value={reason}
              />
            </label>
            <div className="quick-actions">
              <button
                className="secondary-button"
                onClick={() => onNavigate('/patient')}
                type="button"
              >
                Back to dashboard
              </button>
              <button className="primary-button" onClick={handleBooking} type="button">
                Book selected slot
              </button>
            </div>
          </section>
        )}
      </section>
    </main>
  )
}
