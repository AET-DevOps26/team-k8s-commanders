package com.caredesk.notification.controller;

import com.caredesk.notification.service.NotificationsService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.api.AppointmentsApi;
import org.openapitools.model.PaginatedNotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Appointment-scoped notification listing.
 *
 * <p>{@code listAppointmentNotifications} lives on the OpenAPI
 * {@code Appointments} tag, so it is generated onto {@link AppointmentsApi}.
 * This service only owns the {@code /appointments/{appointmentId}/notifications}
 * sub-path — the gateway routes that path here, the clinical note sub-path to
 * the notes-service, and the remaining {@code /appointments/**} routes to the
 * patient-service. The other {@link AppointmentsApi} methods keep their
 * generated 501 defaults and are never reached through the gateway.
 */
@Controller
public class AppointmentNotificationsController implements AppointmentsApi {

    private final NotificationsService notificationsService;
    private final HttpServletRequest request;

    /**
     * @param notificationsService notification business logic
     * @param request              request-scoped proxy used to read the trusted
     *                             gateway headers for the current call
     */
    public AppointmentNotificationsController(NotificationsService notificationsService,
                                              HttpServletRequest request) {
        this.notificationsService = notificationsService;
        this.request = request;
    }

    /**
     * Lists the notifications tied to an appointment. An admin sees all of
     * them; a patient only the ones addressed to them.
     *
     * @param appointmentId the appointment id
     * @param page          zero-based page index
     * @param size          page size
     * @return 200 with one page of notifications
     */
    @Override
    public ResponseEntity<PaginatedNotificationResponse> listAppointmentNotifications(UUID appointmentId,
                                                                                      Integer page,
                                                                                      Integer size) {
        return ResponseEntity.ok(notificationsService.listForAppointment(
                appointmentId,
                GatewayIdentity.role(request),
                GatewayIdentity.userId(request),
                page,
                size));
    }
}
