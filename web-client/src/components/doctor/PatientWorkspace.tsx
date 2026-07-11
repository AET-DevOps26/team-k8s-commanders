import { useEffect, useMemo, useState } from 'react'
import type { Appointment, UserProfile, VisitHistory } from '../../clientApi'
import {
  getAppointmentNote,
  getPatientAppointments,
  getPatientProfile,
  getPatientVisitHistory,
} from '../../clientApi'
import { formatAppointmentDate } from '../../lib/dates'
import { userMessage } from '../../lib/messages'
import { EmptyPanel } from '../ui/EmptyPanel'
import { StatusPanel } from '../ui/StatusPanel'
import { SummaryCard } from '../ui/SummaryCard'
import { NoteEditor } from './NoteEditor'
import { PatientAppointmentTimeline } from './PatientAppointmentTimeline'
import { byDateDesc, isUpcomingAppointment } from './doctorUtils'

type PatientRecordData = {
  profile: UserProfile
  appointments: Appointment[]
  visitHistory: VisitHistory
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

  useEffect(() => {
    if (!patientId) {
      onLoadingChange?.(false)
      return
    }
    const activePatientId = patientId
    let isActive = true

    async function loadRecord() {
      setLoading(true)
      setError('')
      onLoadingChange?.(true)

      try {
        const [profile, appointmentsResponse, visitHistory] = await Promise.all([
          getPatientProfile(activePatientId, token),
          getPatientAppointments(activePatientId, token),
          getPatientVisitHistory(activePatientId, token),
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
            visitHistory,
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

  const displayProfile = data?.profile ?? directoryProfile
  const displayName = displayProfile?.name ?? patientId ?? 'Patient'
  const activeSelectedAppointmentId =
    selectedAppointmentId &&
    sortedAppointments.some((appointment) => appointment.id === selectedAppointmentId)
      ? selectedAppointmentId
      : upcomingAppointments[0]?.id ?? sortedAppointments[0]?.id ?? null
  const selectedAppointment =
    sortedAppointments.find((appointment) => appointment.id === activeSelectedAppointmentId) ??
    null

  useEffect(() => {
    if (!patientId) {
      onAiContextChange?.(null)
      return
    }

    onAiContextChange?.({
      appointmentId: activeSelectedAppointmentId,
      patientId,
      patientName: displayName,
    })
  }, [activeSelectedAppointmentId, displayName, onAiContextChange, patientId])

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
              <PatientAppointmentTimeline
                appointments={sortedAppointments}
                selectedAppointmentId={activeSelectedAppointmentId}
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

          {data.visitHistory.notes?.length ? (
            <section className="record-section visit-history-panel">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">History</p>
                  <h3>Recent notes</h3>
                </div>
              </div>
              <div className="note-list">
                {data.visitHistory.notes.map((note) => (
                  <div className="note-item" key={note.id}>
                    <strong>{formatAppointmentDate(note.createdAt)}</strong>
                    <p>{note.content}</p>
                    {note.diagnosis && (
                      <span>
                        {note.diagnosis.code} · {note.diagnosis.description}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </>
      )}
    </section>
  )
}
