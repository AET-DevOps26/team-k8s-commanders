import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Appointment, AuthSession, UserProfile } from '../../clientApi'
import { DoctorDashboard } from '../../components/doctor/DoctorDashboard'
import { DoctorPatientsPage } from '../../components/doctor/DoctorPatientsPage'
import { DoctorSchedulePage } from '../../components/doctor/DoctorSchedulePage'
import { DoctorSchedulePanel } from '../../components/doctor/DoctorSchedulePanel'
import { PatientDirectory } from '../../components/doctor/PatientDirectory'
import { buildPatientDirectory, buildPatientSummaries } from '../../components/doctor/doctorUtils'
import {
  doctorUser,
  isoDaysFromNow,
  paginated,
  pastAppointment,
  patientSession,
  patientUser,
  upcomingAppointment,
} from '../fixtures'
import { HttpResponse, http, server } from '../server'

const doctorSession: AuthSession = { accessToken: 'doctor-token', user: doctorUser }
const endOfToday = new Date()
endOfToday.setHours(23, 59, 59, 999)
const secondPatient: UserProfile = {
  id: '99999999-9999-4999-8999-999999999999',
  name: 'Berta Beispiel',
  email: 'berta@example.com',
  role: 'PATIENT',
}
const todayAppointment: Appointment = {
  ...upcomingAppointment,
  id: 'today-appointment',
  dateTime: endOfToday.toISOString(),
  reason: 'Today visit',
}
const secondAppointment: Appointment = {
  ...upcomingAppointment,
  id: 'second-appointment',
  patientId: secondPatient.id,
  dateTime: isoDaysFromNow(10),
  reason: 'Second patient visit',
}

function installDoctorDirectoryApi(appointments = [todayAppointment, pastAppointment, secondAppointment]) {
  let appointmentReads = 0
  server.use(
    http.get('*/api/v1/appointments', () => {
      appointmentReads += 1
      return HttpResponse.json(paginated(appointments))
    }),
    http.get('*/api/v1/users', () =>
      HttpResponse.json(paginated([doctorUser, patientUser, secondPatient])),
    ),
  )
  return () => appointmentReads
}

describe('doctor dashboard and records', () => {
  afterEach(() => vi.restoreAllMocks())

  it('loads dashboard summaries, filters schedule, navigates, and refreshes', async () => {
    const user = userEvent.setup()
    const reads = installDoctorDirectoryApi()
    const onNavigate = vi.fn()
    render(
      <DoctorDashboard
        session={doctorSession}
        onLogout={vi.fn()}
        onNavigate={onNavigate}
        bookingSuccess
      />,
    )

    expect(await screen.findByText('Appointment booked successfully.')).toBeInTheDocument()
    expect(screen.getByText(/Today visit/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Upcoming' }))
    expect(screen.getByText(/Second patient visit/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'All' }))
    expect(screen.getByText(new RegExp(pastAppointment.reason!))).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Book for patient' }))
    await user.click(screen.getByRole('button', { name: 'Manage availability' }))
    await user.click(screen.getByRole('button', { name: 'Open patient records' }))
    expect(onNavigate).toHaveBeenCalledWith('/doctor/book')
    expect(onNavigate).toHaveBeenCalledWith('/doctor/schedule')
    expect(onNavigate).toHaveBeenCalledWith('/doctor/patients')

    await user.click(screen.getByRole('button', { name: 'Refresh' }))
    await waitFor(() => expect(reads()).toBe(2))
  })

  it('shows dashboard load and role errors', async () => {
    server.use(
      http.get('*/api/v1/appointments', () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
      http.get('*/api/v1/users', () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
    )
    const { rerender } = render(
      <DoctorDashboard session={doctorSession} onLogout={vi.fn()} onNavigate={vi.fn()} />,
    )
    expect(await screen.findByText(/Doctor data could not be loaded/)).toBeInTheDocument()

    rerender(
      <DoctorDashboard session={patientSession} onLogout={vi.fn()} onNavigate={vi.fn()} />,
    )
    expect(screen.getByRole('heading', { name: 'Doctor account required.' })).toBeInTheDocument()
  })

  it('searches and selects patients in directory', async () => {
    const user = userEvent.setup()
    const summaries = buildPatientSummaries(
      [patientUser, secondPatient],
      [upcomingAppointment, secondAppointment],
    )
    const queue = buildPatientDirectory(summaries)
    const onSelectPatient = vi.fn()
    render(
      <PatientDirectory
        summaries={summaries}
        queue={queue}
        selectedPatientId={patientUser.id}
        onSelectPatient={onSelectPatient}
      />,
    )

    await user.type(screen.getByPlaceholderText('Search patient'), 'berta')
    expect(screen.queryByText(patientUser.email)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Berta Beispiel/ }))
    expect(onSelectPatient).toHaveBeenCalledWith(secondPatient.id)
    await user.clear(screen.getByPlaceholderText('Search patient'))
    await user.type(screen.getByPlaceholderText('Search patient'), 'missing')
    expect(screen.getByText('No patients match this search.')).toBeInTheDocument()
  })

  it('loads patient page and switches active patient workspace', async () => {
    const user = userEvent.setup()
    installDoctorDirectoryApi([upcomingAppointment, secondAppointment])
    Object.defineProperty(window, 'scrollTo', { value: vi.fn(), configurable: true })
    server.use(
      http.get('*/api/v1/patients/:patientId', ({ params }) => {
        const profile = params.patientId === secondPatient.id ? secondPatient : patientUser
        return HttpResponse.json(profile)
      }),
      http.get('*/api/v1/patients/:patientId/appointments', ({ params }) => {
        const appointments = params.patientId === secondPatient.id
          ? [secondAppointment]
          : [upcomingAppointment]
        return HttpResponse.json(paginated(appointments))
      }),
      http.get('*/api/v1/appointments/:appointmentId/note', () =>
        new HttpResponse(null, { status: 204 }),
      ),
    )
    render(
      <DoctorPatientsPage session={doctorSession} onLogout={vi.fn()} onNavigate={vi.fn()} />,
    )

    await screen.findByRole('heading', { name: patientUser.name })
    await user.click(screen.getByRole('button', { name: /Berta Beispiel/ }))
    expect(await screen.findByRole('heading', { name: secondPatient.name })).toBeInTheDocument()
    expect(window.scrollTo).toHaveBeenCalled()
  })
})

describe('doctor schedule page', () => {
  afterEach(() => vi.restoreAllMocks())

  it('loads and removes available slots', async () => {
    const user = userEvent.setup()
    const futureSlot = {
      id: 'slot-to-delete',
      startAt: isoDaysFromNow(4, 9),
      endAt: isoDaysFromNow(4, 10),
      available: true,
    }
    const deleted: string[] = []
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    server.use(
      http.get(`*/api/v1/doctors/${doctorUser.id}/schedule`, () =>
        HttpResponse.json({ doctorId: doctorUser.id, slots: [futureSlot] }),
      ),
      http.delete(`*/api/v1/doctors/${doctorUser.id}/schedule/:slotId`, ({ params }) => {
        deleted.push(String(params.slotId))
        return new HttpResponse(null, { status: 204 })
      }),
    )
    render(
      <DoctorSchedulePage session={doctorSession} onLogout={vi.fn()} onNavigate={vi.fn()} />,
    )

    await user.click(await screen.findByRole('button', { name: 'Remove' }))
    await waitFor(() => expect(deleted).toEqual(['slot-to-delete']))
    expect(screen.getByText('No future slots published.')).toBeInTheDocument()
  })

  it('shows schedule load, delete, and role failures', async () => {
    server.use(
      http.get(`*/api/v1/doctors/${doctorUser.id}/schedule`, () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
      http.get(`*/api/v1/doctors/${patientSession.user.id}/schedule`, () =>
        HttpResponse.json({ doctorId: patientSession.user.id, slots: [] }),
      ),
    )
    const { rerender } = render(
      <DoctorSchedulePage session={doctorSession} onLogout={vi.fn()} onNavigate={vi.fn()} />,
    )
    expect(await screen.findByText(/Availability could not be loaded/)).toBeInTheDocument()

    rerender(
      <DoctorSchedulePage session={patientSession} onLogout={vi.fn()} onNavigate={vi.fn()} />,
    )
    expect(screen.getByRole('heading', { name: 'Doctor account required.' })).toBeInTheDocument()
  })

  it('renders empty schedule filters and opens patient row', async () => {
    const user = userEvent.setup()
    const onOpenPatient = vi.fn()
    render(
      <DoctorSchedulePanel
        appointments={[upcomingAppointment, pastAppointment]}
        users={new Map([[patientUser.id, patientUser]])}
        onOpenPatient={onOpenPatient}
      />,
    )
    expect(screen.getByText('No appointments in this view.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Upcoming' }))
    const row = screen
      .getByText(new RegExp(upcomingAppointment.reason!))
      .closest('.doctor-appointment-row') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Open patient' }))
    expect(onOpenPatient).toHaveBeenCalledWith(patientUser.id)
  })
})
