import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ClinicalNoteInput } from '../../clientApi'
import {
  pastAppointment,
  patientUser,
  upcomingAppointment,
} from '../../test/fixtures'
import { HttpResponse, http, server } from '../../test/server'
import { NoteEditor } from './NoteEditor'
import { PatientWorkspace } from './PatientWorkspace'

function installRecordApi() {
  const saved: ClinicalNoteInput[] = []
  server.use(
    http.get(`*/api/v1/patients/${patientUser.id}`, () => HttpResponse.json(patientUser)),
    http.get(`*/api/v1/appointments/${upcomingAppointment.id}/note`, () =>
      HttpResponse.json({
        id: 'note-1',
        appointmentId: upcomingAppointment.id,
        content: 'Existing clinical note',
        diagnosis: { code: 'J06.9', description: 'Respiratory infection' },
      }),
    ),
    http.get(`*/api/v1/appointments/${pastAppointment.id}/note`, () =>
      new HttpResponse(null, { status: 204 }),
    ),
    http.put(`*/api/v1/appointments/${upcomingAppointment.id}/note`, async ({ request }) => {
      const payload = (await request.json()) as ClinicalNoteInput
      saved.push(payload)
      return HttpResponse.json({
        id: 'note-1',
        appointmentId: upcomingAppointment.id,
        ...payload,
      })
    }),
    http.post('*/api/v1/ai/sessions', () =>
      HttpResponse.json({ id: 'ai-session', messages: [] }, { status: 201 }),
    ),
    http.post('*/api/v1/ai/sessions/ai-session/messages', () =>
      HttpResponse.json({ answer: 'AI generated draft', sources: [] }),
    ),
  )
  return saved
}

describe('patient records', () => {
  it('loads record, filters timeline, saves note, and inserts AI draft', async () => {
    const user = userEvent.setup()
    const saved = installRecordApi()
    const onAiContextChange = vi.fn()
    const onLoadingChange = vi.fn()
    render(
      <PatientWorkspace
        patientId={patientUser.id}
        directoryProfile={patientUser}
        onAiContextChange={onAiContextChange}
        onLoadingChange={onLoadingChange}
        token="doctor-token"
      />,
    )

    expect(await screen.findByDisplayValue('Existing clinical note')).toBeInTheDocument()
    expect(screen.getByText('Clinical notes')).toBeInTheDocument()
    expect(onLoadingChange).toHaveBeenCalledWith(true)
    expect(onLoadingChange).toHaveBeenLastCalledWith(false)
    expect(onAiContextChange).toHaveBeenLastCalledWith(expect.objectContaining({
      appointmentId: upcomingAppointment.id,
      patientId: patientUser.id,
    }))

    await user.selectOptions(screen.getByLabelText('Status'), 'COMPLETED')
    expect(screen.getByText(pastAppointment.reason!)).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Sort'), 'oldest')
    await user.selectOptions(screen.getByLabelText('Status'), 'ALL')

    const note = screen.getByLabelText('Note')
    await user.clear(note)
    await user.type(note, 'Updated note')
    await user.clear(screen.getByLabelText('Diagnosis code'))
    await user.type(screen.getByLabelText('Diagnosis code'), 'A01')
    await user.clear(screen.getByLabelText('Diagnosis description'))
    await user.type(screen.getByLabelText('Diagnosis description'), 'Diagnosis')
    await user.click(screen.getByRole('button', { name: 'Save note' }))

    await waitFor(() => expect(saved).toEqual([{
      content: 'Updated note',
      diagnosis: { code: 'A01', description: 'Diagnosis' },
    }]))
    expect(screen.getByText('Clinical note saved.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Draft with AI' }))
    await waitFor(() => expect(note).toHaveValue('Updated note\n\nAI generated draft'))
    expect(screen.getByText('AI draft inserted. Review before saving.')).toBeInTheDocument()
  })

  it('switches appointments and exposes empty filtered results', async () => {
    const user = userEvent.setup()
    installRecordApi()
    render(
      <PatientWorkspace
        patientId={patientUser.id}
        directoryProfile={patientUser}
        token="doctor-token"
      />,
    )

    await screen.findByDisplayValue('Existing clinical note')
    await user.selectOptions(screen.getByLabelText('Status'), 'CANCELLED')
    expect(screen.getByText('No appointments match this filter.')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Status'), 'ALL')
    const past = screen.getByText(pastAppointment.reason!).closest('button')!
    await user.click(past)
    expect(await screen.findByRole('heading', { name: 'Appointment note' })).toBeInTheDocument()
  })

  it('renders empty selection and record load failures', async () => {
    const onAiContextChange = vi.fn()
    const { rerender } = render(
      <PatientWorkspace
        patientId={null}
        directoryProfile={null}
        onAiContextChange={onAiContextChange}
        token="doctor-token"
      />,
    )
    expect(screen.getByText('Select a patient to open their individual view.')).toBeInTheDocument()
    expect(onAiContextChange).toHaveBeenCalledWith(null)

    server.use(
      http.get(`*/api/v1/patients/${patientUser.id}`, () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
    )
    rerender(
      <PatientWorkspace
        patientId={patientUser.id}
        directoryProfile={patientUser}
        token="doctor-token"
      />,
    )
    expect(await screen.findByText(/Patient record could not be loaded/)).toBeInTheDocument()
  })

  it('surfaces note load, save, and AI failures', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(`*/api/v1/appointments/${upcomingAppointment.id}/note`, () =>
        HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
      ),
      http.put(`*/api/v1/appointments/${upcomingAppointment.id}/note`, () =>
        HttpResponse.json({ detail: 'Rejected' }, { status: 400 }),
      ),
      http.post('*/api/v1/ai/sessions', () =>
        HttpResponse.json({ detail: 'AI offline' }, { status: 503 }),
      ),
    )
    render(
      <NoteEditor
        appointmentId={upcomingAppointment.id}
        patientId={patientUser.id}
        token="doctor-token"
      />,
    )

    expect(await screen.findByText(/Existing note could not be loaded/)).toBeInTheDocument()
    await user.type(screen.getByLabelText('Note'), 'New content')
    await user.click(screen.getByRole('button', { name: 'Save note' }))
    expect(await screen.findByText(/Clinical note could not be saved/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Draft with AI' }))
    expect(await screen.findByText(/AI draft could not be generated/)).toBeInTheDocument()
  })
})
