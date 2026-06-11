package com.caredesk.notification.controller;

import com.caredesk.notification.service.NotificationsService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.api.NotificationsApi;
import org.openapitools.model.Notification;
import org.openapitools.model.NotificationCreate;
import org.openapitools.model.PaginatedNotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Notification endpoints for the notification service.
 *
 * <p>Implements the generated {@link NotificationsApi}: creating a
 * notification record (admin only, enforced in {@code SecurityConfig}) and
 * the role-scoped list/get reads. The caller's identity comes from the
 * trusted gateway headers via {@link GatewayIdentity}.
 */
@Controller
public class NotificationsController implements NotificationsApi {

    private final NotificationsService notificationsService;
    private final HttpServletRequest request;

    /**
     * @param notificationsService notification business logic
     * @param request              request-scoped proxy used to read the trusted
     *                             gateway headers for the current call
     */
    public NotificationsController(NotificationsService notificationsService, HttpServletRequest request) {
        this.notificationsService = notificationsService;
        this.request = request;
    }

    /**
     * Records a notification.
     *
     * @param notificationCreate the message, channel and optional patient /
     *                           appointment references
     * @return 201 with the persisted notification
     */
    @Override
    public ResponseEntity<Notification> createNotification(NotificationCreate notificationCreate) {
        String message = notificationCreate.getMessage();
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required");
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notificationsService.create(notificationCreate));
    }

    /**
     * Returns a single notification. Patients may only read their own.
     *
     * @param notificationId the notification id
     * @return 200 with the notification, 404 if unknown, 403 if not the recipient
     */
    @Override
    public ResponseEntity<Notification> getNotificationById(UUID notificationId) {
        return ResponseEntity.ok(notificationsService.getById(
                notificationId,
                GatewayIdentity.role(request),
                GatewayIdentity.userId(request)));
    }

    /**
     * Lists notifications visible to the caller: all for an admin, only
     * their own for a patient.
     *
     * @param page zero-based page index
     * @param size page size
     * @return 200 with one page of notifications
     */
    @Override
    public ResponseEntity<PaginatedNotificationResponse> listNotifications(Integer page, Integer size) {
        return ResponseEntity.ok(notificationsService.list(
                GatewayIdentity.role(request),
                GatewayIdentity.userId(request),
                page,
                size));
    }
}
