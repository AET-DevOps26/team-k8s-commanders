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
 * {@code PatientHeaderAuthFilter}). Per-role ownership rules are deferred to
 * issue #32.
 *
 * <p>The appointment-note endpoints ({@code getAppointmentNote} and
 * {@code upsertAppointmentNote}) are not overridden here, they belong to the
 * future clinical-notes service. They keep returning 501 via the OpenAPI
 * default implementation until then.
 */
@Controller
public class AppointmentsController implements AppointmentsApi {

    /** Trusted email injected by the gateway after it validates the JWT. */
    static final String USER_EMAIL_HEADER = "X-User-Email";

    private final AppointmentService appointmentService;
    private final HttpServletRequest request;

    /**
     * @param appointmentService the read / write appointment service
     * @param request            request-scoped proxy used to read the trusted
     *                           {@code X-User-Email} header for the current call
     */
    public AppointmentsController(AppointmentService appointmentService, HttpServletRequest request) {
        this.appointmentService = appointmentService;
        this.request = request;
    }

    /**
     * Books a new appointment. The caller's gateway-provided email is captured
     * on the appointment so notification-service can reach the patient.
     *
     * @param appointmentCreate the booking request
     * @return 201 with the newly created appointment
     */
    @Override
    public ResponseEntity<Appointment> bookAppointment(AppointmentCreate appointmentCreate) {
        String contactEmail = request.getHeader(USER_EMAIL_HEADER);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.book(appointmentCreate, contactEmail));
    }

    /**
     * Lists all appointments, paged.
     *
     * @param page zero-based page index, defaulted to 0 by the API
     * @param size page size, defaulted to 20 by the API
     * @return 200 with the page of appointments
     */
    @Override
    public ResponseEntity<PaginatedAppointmentResponse> listAppointments(Integer page, Integer size) {
        return ResponseEntity.ok(appointmentService.list(page, size));
    }

    /**
     * Returns a single appointment.
     *
     * @param appointmentId the appointment id
     * @return 200 with the appointment, or 404 if unknown
     */
    @Override
    public ResponseEntity<Appointment> getAppointmentById(UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.getById(appointmentId));
    }

    /**
     * Reschedules an existing appointment to a new date / time.
     *
     * @param appointmentId                the appointment id
     * @param appointmentRescheduleRequest the new date / time and optional duration
     * @return 200 with the updated appointment, or 404 if unknown
     */
    @Override
    public ResponseEntity<Appointment> rescheduleAppointment(
            UUID appointmentId, AppointmentRescheduleRequest appointmentRescheduleRequest) {
        return ResponseEntity.ok(appointmentService.reschedule(appointmentId, appointmentRescheduleRequest));
    }

    /**
     * Cancels an appointment.
     *
     * @param appointmentId the appointment id
     * @return 200 with the cancelled appointment, or 404 if unknown
     */
    @Override
    public ResponseEntity<Appointment> cancelAppointment(UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.cancel(appointmentId));
    }
}
