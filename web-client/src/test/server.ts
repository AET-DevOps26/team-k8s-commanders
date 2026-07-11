import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import type { AppointmentCreate, LoginRequest, RegisterRequest } from '../clientApi'
import {
  PATIENT_PASSWORD,
  doctorSchedule,
  doctorUser,
  isoDaysFromNow,
  paginated,
  pastAppointment,
  patientSession,
  patientUser,
  upcomingAppointment,
  visitHistory,
} from './fixtures'

/**
 * Default happy-path handlers for the API gateway. Individual tests override
 * behaviour with `server.use(...)`.
 */
export const handlers = [
  http.post('*/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginRequest

    if (body.email === patientUser.email && body.password === PATIENT_PASSWORD) {
      return HttpResponse.json(patientSession)
    }

    return HttpResponse.json(
      { detail: 'Invalid credentials' },
      { status: 401 },
    )
  }),

  http.post('*/api/v1/auth/register', async ({ request }) => {
    const body = (await request.json()) as RegisterRequest

    return HttpResponse.json(
      {
        accessToken: 'fresh-register-token',
        user: {
          ...patientUser,
          name: body.name,
          email: body.email,
          phoneNumber: body.phoneNumber,
          dateOfBirth: body.dateOfBirth,
        },
      },
      { status: 201 },
    )
  }),

  http.post('*/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 })),

  http.get(`*/api/v1/users/${patientUser.id}`, () =>
    HttpResponse.json(patientUser),
  ),

  http.get(`*/api/v1/patients/${patientUser.id}/appointments`, () =>
    HttpResponse.json(paginated([upcomingAppointment, pastAppointment])),
  ),

  http.get(`*/api/v1/patients/${patientUser.id}/visit-history`, () =>
    HttpResponse.json(visitHistory),
  ),

  http.get('*/api/v1/doctors', () => HttpResponse.json(paginated([doctorUser]))),

  http.get('*/api/v1/doctors/specializations', () =>
    HttpResponse.json([doctorUser.specialization ?? 'General Medicine']),
  ),

  http.get(`*/api/v1/doctors/${doctorUser.id}/schedule`, () =>
    HttpResponse.json(doctorSchedule),
  ),

  http.post('*/api/v1/appointments', async ({ request }) => {
    const body = (await request.json()) as AppointmentCreate

    return HttpResponse.json(
      {
        id: '66666666-6666-4666-8666-666666666666',
        patientId: body.patientId,
        doctorId: body.doctorId,
        dateTime: body.dateTime,
        status: 'SCHEDULED',
        duration: body.duration,
        reason: body.reason,
      },
      { status: 201 },
    )
  }),

  http.post(`*/api/v1/appointments/${upcomingAppointment.id}/cancel`, () =>
    HttpResponse.json({ ...upcomingAppointment, status: 'CANCELLED' }),
  ),

  http.put(`*/api/v1/appointments/${upcomingAppointment.id}`, async ({ request }) => {
    const body = (await request.json()) as { dateTime: string; duration?: number }

    return HttpResponse.json({
      ...upcomingAppointment,
      dateTime: body.dateTime,
      duration: body.duration ?? upcomingAppointment.duration,
      status: 'RESCHEDULED',
    })
  }),
]

export const server = setupServer(...handlers)

export { HttpResponse, http, isoDaysFromNow }
