import { useEffect, useState } from 'react'
import type { ScheduleSlot, UserProfile } from '../../clientApi'
import {
  bookAppointment,
  getDoctorSchedule,
  listDoctorSpecializations,
  listDoctors,
} from '../../clientApi'
import { isPastDateTime } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import type { PatientBookingPageProps } from '../../types/route'
import { BookingCalendarSection } from '../booking/BookingCalendarSection'
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
  const [specializations, setSpecializations] = useState<string[]>([])
  const [specialization, setSpecialization] = useState('')
  const [doctors, setDoctors] = useState<UserProfile[]>([])
  const [selectedDoctor, setSelectedDoctor] = useState<UserProfile | null>(null)
  const [slots, setSlots] = useState<ScheduleSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<ScheduleSlot | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [isLoadingSpecializations, setLoadingSpecializations] = useState(false)
  const [isLoadingDoctors, setLoadingDoctors] = useState(false)

  function handleSpecializationChange(value: string) {
    setSpecialization(value)
    setDoctors([])
    setSelectedDoctor(null)
    setSlots([])
    setSelectedSlot(null)
    setError('')
  }

  function handleDoctorChange(doctorId: string) {
    const doctor = doctors.find((candidate) => candidate.id === doctorId)

    if (!doctor) {
      setSelectedDoctor(null)
      setSlots([])
      setSelectedSlot(null)
      return
    }

    void selectDoctor(doctor)
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
    let active = true

    async function loadSpecializations() {
      setLoadingSpecializations(true)
      setError('')

      try {
        const response = await listDoctorSpecializations(session.accessToken)
        if (active) {
          setSpecializations(response)
        }
      } catch {
        if (active) {
          setError(userMessage('Specializations could not be loaded. Please try again in a moment.'))
        }
      } finally {
        if (active) {
          setLoadingSpecializations(false)
        }
      }
    }

    loadSpecializations()

    return () => {
      active = false
    }
  }, [session.accessToken])

  useEffect(() => {
    let active = true

    if (!specialization) {
      return () => {
        active = false
      }
    }

    async function loadDoctors() {
      setLoadingDoctors(true)
      setError('')

      try {
        const response = await listDoctors(session.accessToken, {
          specialization,
          size: 100,
        })
        if (active) {
          setDoctors(response.content)
        }
      } catch {
        if (active) {
          setError(userMessage('Doctors could not be loaded. Please try again in a moment.'))
        }
      } finally {
        if (active) {
          setLoadingDoctors(false)
        }
      }
    }

    loadDoctors()

    return () => {
      active = false
    }
  }, [session.accessToken, specialization])

  function searchHelperText() {
    if (isLoadingSpecializations) {
      return 'Loading specializations.'
    }

    if (!specializations.length) {
      return 'No specializations available yet.'
    }

    if (!specialization) {
      return 'Choose a specialization to load doctors.'
    }

    if (isLoadingDoctors) {
      return 'Loading doctors.'
    }

    if (!doctors.length) {
      return 'No doctors found for this specialization.'
    }

    if (!selectedDoctor) {
      return 'Choose a doctor to view available slots.'
    }

    return ''
  }

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
          searchLabel="Specialization"
          searchPlaceholder={
            isLoadingSpecializations
              ? 'Loading specializations'
              : specializations.length
                ? 'Select specialization'
                : 'No specializations available'
          }
          searchValue={specialization}
          onSearchChange={handleSpecializationChange}
          searchOptions={specializations.map((name) => ({ value: name, label: name }))}
          searchDisabled={isLoadingSpecializations || !specializations.length}
          secondaryLabel="Doctor"
          secondaryPlaceholder={specialization ? 'Select doctor' : 'Select specialization first'}
          secondaryValue={selectedDoctor?.id ?? ''}
          onSecondaryChange={handleDoctorChange}
          secondaryOptions={doctors.map((doctor) => ({ value: doctor.id, label: doctor.name }))}
          secondaryDisabled={!specialization || isLoadingDoctors || !doctors.length}
          onSubmit={(event) => event?.preventDefault()}
          isLoading={isLoadingSpecializations || isLoadingDoctors}
          showSubmit={false}
          helperText={searchHelperText()}
          layout="inline"
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
