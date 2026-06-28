import { FormEvent, useEffect, useState } from 'react'
import type { ScheduleSlot, UserProfile } from '../../clientApi'
import {
  bookAppointment,
  getDoctorSchedule,
  listDoctors,
} from '../../clientApi'
import { isPastDateTime } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import type { PatientBookingPageProps } from '../../types/route'
import { BookingCalendarSection } from '../booking/BookingCalendarSection'
import { BookingEntityResults } from '../booking/BookingEntityResults'
import { BookingSearchSection } from '../booking/BookingSearchSection'
import { PatientSubNav } from '../layout/PatientSubNav'
import { ShellNav } from '../layout/ShellNav'
import { StatusPanel } from '../ui/StatusPanel'

export function PatientBookingPage({
  session,
  onLogout,
  onNavigate,
  onBooked,
}: PatientBookingPageProps) {
  const [query, setQuery] = useState('')
  const [specialization, setSpecialization] = useState('')
  const [doctors, setDoctors] = useState<UserProfile[]>([])
  const [selectedDoctor, setSelectedDoctor] = useState<UserProfile | null>(null)
  const [slots, setSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(false)

  async function searchDoctors(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    setLoading(true)
    setError('')

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

    try {
      const schedule = await getDoctorSchedule(doctor.id, session.accessToken)
      const availableFutureSlots = schedule.slots.filter((slot) => !isPastDateTime(slot.startAt))
      setSlots(availableFutureSlots)
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

    if (isPastDateTime(selectedSlot.startAt)) {
      setSelectedSlot(null)
      setError('Please choose an upcoming time slot')
      return
    }

    setError('')

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
      onBooked()
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

        <BookingSearchSection
          title="Doctor search"
          searchPlaceholder="Name or specialization"
          searchValue={query}
          onSearchChange={setQuery}
          secondaryLabel="Specialization"
          secondaryPlaceholder="General Medicine"
          secondaryValue={specialization}
          onSecondaryChange={setSpecialization}
          onSubmit={searchDoctors}
          isLoading={isLoading}
          submitLabel="Search doctors"
          loadingLabel="Searching"
          results={
            <BookingEntityResults
              entities={doctors}
              selectedEntityId={selectedDoctor?.id ?? null}
              onSelectEntity={selectDoctor}
              emptyMessage="No doctors found. Try another search."
              subtitle={(doctor) => doctor.specialization ?? 'CareDesk doctor'}
            />
          }
        />

        {selectedDoctor && (
          <BookingCalendarSection
            subjectName={selectedDoctor.name}
            slots={slots}
            selectedSlot={selectedSlot}
            onSelectSlot={setSelectedSlot}
            reason={reason}
            onReasonChange={setReason}
            onCancel={() => onNavigate('/patient')}
            onBook={handleBooking}
            cancelLabel="Back to dashboard"
            bookLabel="Book selected slot"
          />
        )}
      </section>
    </main>
  )
}
