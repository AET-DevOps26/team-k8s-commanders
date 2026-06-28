import { FormEvent, useEffect, useMemo, useState } from 'react'
import type { ScheduleSlot, UserProfile } from '../../clientApi'
import {
  bookAppointment,
  fetchAllUserPages,
  getDoctorSchedule,
} from '../../clientApi'
import { isPastDateTime } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import type { DoctorDashboardProps } from '../../types/route'
import { BookingCalendarSection } from '../booking/BookingCalendarSection'
import { BookingEntityResults } from '../booking/BookingEntityResults'
import { BookingSearchSection } from '../booking/BookingSearchSection'
import { DoctorSubNav } from '../layout/DoctorSubNav'
import { ShellNav } from '../layout/ShellNav'
import { StatusPanel } from '../ui/StatusPanel'

type DoctorBookAppointmentPageProps = DoctorDashboardProps & {
  onBooked: () => void
}

export function DoctorBookAppointmentPage({
  session,
  onLogout,
  onNavigate,
  onBooked,
}: DoctorBookAppointmentPageProps) {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState<UserProfile[]>([])
  const [allPatients, setAllPatients] = useState<UserProfile[]>([])
  const [selectedPatient, setSelectedPatient] = useState<UserProfile | null>(null)
  const [slots, setSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(false)
  const doctorId = session.user.id
  const token = session.accessToken

  const filteredPatients = useMemo(() => {
    const trimmed = query.trim().toLowerCase()

    if (!trimmed) {
      return patients
    }

    return patients.filter(
      (patient) =>
        patient.name.toLowerCase().includes(trimmed) ||
        patient.email.toLowerCase().includes(trimmed),
    )
  }, [patients, query])

  async function loadPatients() {
    setLoading(true)
    setError('')

    try {
      const users = await fetchAllUserPages(token)
      const patientProfiles = users.filter((user) => user.role === 'PATIENT')
      setAllPatients(patientProfiles)
      setPatients(patientProfiles)

      if (selectedPatient && !patientProfiles.some((patient) => patient.id === selectedPatient.id)) {
        setSelectedPatient(null)
        setSelectedSlot(null)
      }
    } catch {
      setError(userMessage('Patients could not be loaded. Please try again in a moment.'))
    } finally {
      setLoading(false)
    }
  }

  async function loadSchedule() {
    try {
      const schedule = await getDoctorSchedule(doctorId, token)
      const availableFutureSlots = schedule.slots.filter((slot) => !isPastDateTime(slot.startAt))
      setSlots(availableFutureSlots)
    } catch {
      setSlots([])
      setError(userMessage('Available times could not be loaded. Please try again.'))
    }
  }

  function searchPatients(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    setPatients(allPatients)
  }

  function selectPatient(patient: UserProfile) {
    setSelectedPatient(patient)
    setSelectedSlot(null)
    setError('')
  }

  async function handleBooking() {
    if (!selectedPatient || !selectedSlot) {
      setError('Please select a patient and time slot')
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
      await bookAppointment(token, {
        patientId: selectedPatient.id,
        doctorId,
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
    loadPatients()
    loadSchedule()
    // Initial patient list and schedule should load once for the active session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, doctorId])

  if (session.user.role !== 'DOCTOR') {
    return (
      <main className="landing-page app-page">
        <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
        <section className="empty-state">
          <p className="eyebrow">Book appointment</p>
          <h1>Doctor account required.</h1>
          <button className="primary-button" onClick={onLogout} type="button">
            Logout
          </button>
        </section>
      </main>
    )
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell doctor-dashboard-shell">
        <header className="patient-hero doctor-hero">
          <div>
            <p className="eyebrow">Booking</p>
            <h1>Book for a patient</h1>
            <p>Choose a patient, pick one of your available slots, and create the appointment.</p>
          </div>
        </header>

        <DoctorSubNav active="book" onNavigate={onNavigate} />

        {error && <StatusPanel title="We could not complete your booking" text={error} />}

        <BookingSearchSection
          title="Patient search"
          searchPlaceholder="Name or email"
          searchValue={query}
          onSearchChange={setQuery}
          onSubmit={searchPatients}
          isLoading={isLoading}
          submitLabel="Search patients"
          loadingLabel="Searching"
          scrollableResults
          results={
            <BookingEntityResults
              entities={filteredPatients}
              selectedEntityId={selectedPatient?.id ?? null}
              onSelectEntity={selectPatient}
              emptyMessage="No patients found. Try another search."
              subtitle={() => 'CareDesk patient'}
            />
          }
        />

        {selectedPatient && (
          <BookingCalendarSection
            subjectName={selectedPatient.name}
            slots={slots}
            selectedSlot={selectedSlot}
            onSelectSlot={setSelectedSlot}
            reason={reason}
            onReasonChange={setReason}
            onCancel={() => onNavigate('/doctor')}
            onBook={handleBooking}
            cancelLabel="Back to dashboard"
            bookLabel="Book selected slot"
          />
        )}
      </section>
    </main>
  )
}
