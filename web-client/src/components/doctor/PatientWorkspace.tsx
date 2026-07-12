import { useEffect, useMemo, useState } from 'react'
import type { Appointment, UserProfile } from '../../clientApi'
import {
  getAppointmentNote,
  getPatientAppointments,
  getPatientProfile,
} from '../../clientApi'
import { formatAppointmentDate } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import { EmptyPanel } from '../ui/EmptyPanel'
import { StatusPanel } from '../ui/StatusPanel'
import { SummaryCard } from '../ui/SummaryCard'
import {
  AppointmentFilterBar,
  type AppointmentSortOrder,
  type AppointmentStatusFilter,
} from '../appointments/AppointmentFilterBar'
import { NoteEditor } from './NoteEditor'
import { PatientAppointmentTimeline } from './PatientAppointmentTimeline'
import { byDateAsc, byDateDesc, isUpcomingAppointment } from './doctorUtils'

type PatientRecordData = {
  profile: UserProfile
  appointments: Appointment[]
}

type PatientWorkspaceProps = {
  patientId: string | null
  directoryProfile: UserProfile | null
  onAiContextChange?: (context: PatientAiContext | null) => void
  onLoadingChange?: (isLoading: boolean) => void
  token: string
}

export type PatientAiContext = {
  appointmentId: string | null
  patientId: string
  patientName: string
}

export function PatientWorkspace({
  patientId,
  directoryProfile,
  onAiContextChange,
  onLoadingChange,
  token,
}: PatientWorkspaceProps) {
  const [data, setData] = useState<PatientRecordData | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(false)
  const [notedAppointmentIds, setNotedAppointmentIds] = useState<Set<string>>(
    () => new Set(),
  )
  const [selectedAppointmentId, setSelectedAppointmentId] = useState<string | null>(
    null,
  )
  const [statusFilter, setStatusFilter] = useState<AppointmentStatusFilter>('ALL')
  const [sortOrder, setSortOrder] = useState<AppointmentSortOrder>('newest')

  useEffect(() => {
    if (!patientId) {
      setData(null)
      onLoadingChange?.(false)
      return
    }

    setData(null)

    const activePatientId = patientId
    let isActive = true

    setLoading(true)
    setError('')
    onLoadingChange?.(true)

    async function loadRecord() {

      try {
        const [profile, appointmentsResponse] = await Promise.all([
          getPatientProfile(activePatientId, token),
          getPatientAppointments(activePatientId, token),
        ])
        const noteResults = await Promise.all(
          appointmentsResponse.content.map(async (appointment) => {
            try {
              const note = await getAppointmentNote(appointment.id, token)
              return note ? appointment.id : null
            } catch {
              return null
            }
          }),
        )

        if (isActive) {
          setData({
            profile,
            appointments: appointmentsResponse.content,
          })
          setNotedAppointmentIds(
            new Set(
              noteResults.filter((appointmentId): appointmentId is string =>
                Boolean(appointmentId),
              ),
            ),
          )
        }
      } catch {
        if (isActive) {
          setError(userMessage('Patient record could not be loaded. Please try again.'))
          setData(null)
          setNotedAppointmentIds(new Set())
        }
      } finally {
        if (isActive) {
          setLoading(false)
          onLoadingChange?.(false)
        }
      }
    }

    loadRecord()

    return () => {
      isActive = false
    }
  }, [onLoadingChange, patientId, token])

  const sortedAppointments = useMemo(() => {
    if (!data) {
      return []
    }

    return [...data.appointments].sort(byDateDesc)
  }, [data])

  const upcomingAppointments = useMemo(
    () => sortedAppointments.filter((appointment) => isUpcomingAppointment(appointment)),
    [sortedAppointments],
  )

  const visibleAppointments = useMemo(() => {
    const filtered =
      statusFilter === 'ALL'
        ? sortedAppointments
        : sortedAppointments.filter((appointment) => appointment.status === statusFilter)

    return [...filtered].sort(sortOrder === 'oldest' ? byDateAsc : byDateDesc)
  }, [sortOrder, sortedAppointments, statusFilter])

  const displayProfile = data?.profile ?? directoryProfile
  const displayName = displayProfile?.name ?? patientId ?? 'Patient'
  const selectedAppointment =
    sortedAppointments.find((appointment) => appointment.id === selectedAppointmentId) ??
    null

  useEffect(() => {
    if (!sortedAppointments.length) {
      setSelectedAppointmentId(null)
      return
    }

    setSelectedAppointmentId((current) => {
      if (current && sortedAppointments.some((appointment) => appointment.id === current)) {
        return current
      }

      return upcomingAppointments[0]?.id ?? sortedAppointments[0].id
    })
  }, [sortedAppointments, upcomingAppointments])

  useEffect(() => {
    if (!patientId) {
      onAiContextChange?.(null)
      return
    }

    onAiContextChange?.({
      appointmentId: selectedAppointmentId,
      patientId,
      patientName: displayName,
    })
  }, [displayName, onAiContextChange, patientId, selectedAppointmentId])

  function handleNoteSaved(appointmentId: string) {
    setNotedAppointmentIds((current) => {
      const next = new Set(current)
      next.add(appointmentId)
      return next
    })
  }

  if (!patientId) {
    return (
      <section className="dashboard-panel patient-workspace empty-workspace">
        <EmptyPanel text="Select a patient to open their individual view." />
      </section>
    )
  }

  return (
    <section className={`dashboard-panel patient-workspace${isLoading ? ' is-loading' : ''}`}>
      <div className="patient-workspace-head">
        <div>
          <p className="eyebrow">Patient view</p>
          <h2>{displayName}</h2>
          {displayProfile?.email && <p>{displayProfile.email}</p>}
        </div>
        <dl className="patient-mini-profile">
          <div>
            <dt>Date of birth</dt>
            <dd>{displayProfile?.dateOfBirth ?? 'Not provided'}</dd>
          </div>
          <div>
            <dt>Phone</dt>
            <dd>{displayProfile?.phoneNumber ?? 'Not provided'}</dd>
          </div>
        </dl>
      </div>

      {isLoading && <StatusPanel title="Loading patient record" />}
      {error && <StatusPanel title="Patient record unavailable" text={error} />}

      {data && (
        <>
          <div className="patient-record-summary">
            <SummaryCard
              label="Open"
              value={String(upcomingAppointments.length)}
              text="Upcoming appointments"
            />
            <SummaryCard
              label="Total"
              value={String(sortedAppointments.length)}
              text="Appointments in record"
            />
            <SummaryCard
              label="Notes"
              value={String(notedAppointmentIds.size)}
              text="Clinical notes"
            />
          </div>

          <section className="patient-record-workbench">
            <aside className="record-section patient-visit-list">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Timeline</p>
                  <h3>Appointments</h3>
                </div>
              </div>
              <AppointmentFilterBar
                statusFilter={statusFilter}
                sortOrder={sortOrder}
                onStatusFilterChange={setStatusFilter}
                onSortOrderChange={setSortOrder}
              />
              <PatientAppointmentTimeline
                appointments={visibleAppointments}
                emptyText={
                  statusFilter === 'ALL'
                    ? 'No appointments for this patient.'
                    : 'No appointments match this filter.'
                }
                selectedAppointmentId={selectedAppointmentId}
                onSelectAppointment={setSelectedAppointmentId}
              />
            </aside>

            <div className="patient-care-column">
              <section className="record-section selected-appointment-panel">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Selected appointment</p>
                    <h3>
                      {selectedAppointment
                        ? formatAppointmentDate(selectedAppointment.dateTime)
                        : 'No appointment'}
                    </h3>
                  </div>
                </div>
                {selectedAppointment ? (
                  <NoteEditor
                    appointmentId={selectedAppointment.id}
                    key={selectedAppointment.id}
                    patientId={patientId}
                    token={token}
                    onSaved={handleNoteSaved}
                  />
                ) : (
                  <EmptyPanel text="Select an appointment to write a clinical note." />
                )}
              </section>
            </div>
          </section>
        </>
      )}
    </section>
  )
}
