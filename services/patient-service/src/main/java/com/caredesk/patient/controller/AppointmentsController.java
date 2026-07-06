package com.caredesk.patient.controller;

import com.caredesk.patient.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.api.AppointmentsApi;
import org.openapitools.model.Appointment;
import org.openapitools.model.AppointmentCreate;
import org.openapitools.model.AppointmentRescheduleRequest;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controller for the {@code /appointments/**} endpoints.
 *
 * <p>Implements {@link AppointmentsApi} and delegates business logic to
 * {@link AppointmentService}. Authentication is enforced by the
 * gateway-injected {@code X-User-*} headers (see
 * {@code PatientHeaderAuthFilter}); per-record ownership is enforced in the
 * service against the caller identity read via {@link GatewayIdentity}
 * (issue #172).
 *
 * <p>The appointment-note endpoints ({@code getAppointmentNote} and
 * {@code upsertAppointmentNote}) are not overridden here — the gateway routes
 * that sub-path to the notes-service, so the generated 501 defaults are never
 * reached.
 */
@Controller
public class AppointmentsController implements AppointmentsApi {

    private final AppointmentService appointmentService;
    private final HttpServletRequest request;

    /**
     * @param appointmentService the read / write appointment service
     * @param request            request-scoped proxy used to read the trusted
     *                           {@code X-User-*} headers for the current call
     */
    public AppointmentsController(AppointmentService appointmentService, HttpServletRequest request) {
        this.appointmentService = appointmentService;
        this.request = request;
    }

    /**
     * Books a new appointment. A patient may only book for themselves; doctors
     * and admins may book on a patient's behalf. The patient's contact email is
     * resolved from the authoritative user profile inside the service, so a
     * doctor booking on a patient's behalf still notifies the patient rather
     * than the caller.
     *
     * @param appointmentCreate the booking request
     * @return 201 with the newly created appointment
     */
    @Override
    public ResponseEntity<Appointment> bookAppointment(AppointmentCreate appointmentCreate) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.book(appointmentCreate, GatewayIdentity.caller(request)));
    }

    /**
     * Lists appointments scoped to the caller: all of them for an admin, only
     * their own for a doctor. Patients use {@code /patients/{id}/appointments}.
     *
     * @param page zero-based page index, defaulted to 0 by the API
     * @param size page size, defaulted to 20 by the API
     * @return 200 with the page of appointments
     */
    @Override
    public ResponseEntity<PaginatedAppointmentResponse> listAppointments(Integer page, Integer size) {
        return ResponseEntity.ok(appointmentService.list(page, size, GatewayIdentity.caller(request)));
    }

    /**
     * Returns a single appointment. Only its patient, its doctor or an admin
     * may read it.
     *
     * @param appointmentId the appointment id
     * @return 200 with the appointment, 404 if unknown, 403 if not a participant
     */
    @Override
    public ResponseEntity<Appointment> getAppointmentById(UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.getById(appointmentId, GatewayIdentity.caller(request)));
    }

    /**
     * Reschedules an existing appointment to a new date / time. Only its
     * patient, its doctor or an admin may reschedule it.
     *
     * @param appointmentId                the appointment id
     * @param appointmentRescheduleRequest the new date / time and optional duration
     * @return 200 with the updated appointment, or 404 if unknown
     */
    @Override
    public ResponseEntity<Appointment> rescheduleAppointment(
            UUID appointmentId, AppointmentRescheduleRequest appointmentRescheduleRequest) {
        return ResponseEntity.ok(appointmentService.reschedule(
                appointmentId, appointmentRescheduleRequest, GatewayIdentity.caller(request)));
    }

    /**
     * Cancels an appointment. Only its patient, its doctor or an admin may
     * cancel it.
     *
     * @param appointmentId the appointment id
     * @return 200 with the cancelled appointment, or 404 if unknown
     */
    @Override
    public ResponseEntity<Appointment> cancelAppointment(UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.cancel(appointmentId, GatewayIdentity.caller(request)));
    }
}
