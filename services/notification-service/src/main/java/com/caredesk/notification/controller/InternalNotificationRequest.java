package com.caredesk.notification.controller;

import com.caredesk.notification.model.NotificationType;

import java.util.UUID;

/**
 * Body of the service-to-service {@code POST /internal/notifications} call.
 *
 * <p>This is an internal contract between patient-service (the caller) and
 * notification-service, deliberately separate from the public OpenAPI
 * {@code NotificationCreate} model: it carries the recipient address and the
 * internal {@link NotificationType} that the public model does not expose.
 *
 * @param appointmentId  the appointment the notification is about
 * @param patientId      the recipient patient's user id
 * @param recipientEmail the address to deliver to (captured at booking time)
 * @param type           why the notification is being sent
 * @param subject        the email subject line
 * @param message        the message body
 */
public record InternalNotificationRequest(
        UUID appointmentId,
        UUID patientId,
        String recipientEmail,
        NotificationType type,
        String subject,
        String message) {
}
