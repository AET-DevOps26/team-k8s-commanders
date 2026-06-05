package com.caredesk.notes.controller;

import org.openapitools.api.AppointmentsApi;
import org.springframework.stereotype.Controller;

/**
 * Scaffold controller for the clinical note endpoints.
 *
 * <p>The clinical note operations ({@code getAppointmentNote} and
 * {@code upsertAppointmentNote}) live on the OpenAPI {@code Appointments} tag,
 * so they are generated onto {@link AppointmentsApi}. This service only owns
 * the {@code /appointments/{appointmentId}/note} sub-path — the gateway routes
 * that path here and the remaining {@code /appointments/**} routes to the
 * patient-service. Implementing {@link AppointmentsApi} registers the note
 * routes; the default 501 behaviour holds until the real handlers land.
 */
@Controller
public class NotesController implements AppointmentsApi {
}
