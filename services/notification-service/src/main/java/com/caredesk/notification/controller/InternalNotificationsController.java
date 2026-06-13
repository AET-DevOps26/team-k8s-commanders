package com.caredesk.notification.controller;

import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.service.NotificationsService;
import org.openapitools.model.Notification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service-to-service notification endpoint, outside the public OpenAPI contract.
 *
 * <p>patient-service calls {@code POST /internal/notifications} after a booking
 * event (book / reschedule / cancel) to record and deliver a confirmation. The
 * {@code /internal} prefix is not exposed through the API gateway — the gateway
 * only routes the public {@code /notifications} and appointment-notification
 * paths, neither of which match {@code /internal} — so this endpoint is
 * reachable only pod-to-pod, guarded by the cluster NetworkPolicy (only the
 * gateway and patient-service may reach this service) and permitted anonymously
 * in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationsController {

    private final NotificationsService notificationsService;

    /**
     * @param notificationsService notification business logic
     */
    public InternalNotificationsController(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    /**
     * Records and delivers a notification triggered by another service.
     *
     * @param request the trigger payload
     * @return 201 with the persisted notification
     */
    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody InternalNotificationRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required");
        }
        NotificationType type = request.type() != null ? request.type() : NotificationType.GENERIC;
        Notification created = notificationsService.recordAndSend(
                request.appointmentId(),
                request.patientId(),
                request.recipientEmail(),
                type,
                request.subject() != null ? request.subject() : "CareDesk notification",
                request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
