package com.caredesk.patient.service;

import java.util.UUID;

/**
 * Body of the service-to-service {@code POST /internal/notifications} call to
 * notification-service.
 *
 * <p>{@code type} is sent as a string matching notification-service's internal
 * {@code NotificationType} enum name (e.g. {@code "CONFIRMATION"}), so the two
 * services stay decoupled at the type level.
 *
 * @param appointmentId  the appointment the notification is about
 * @param patientId      the recipient patient's user id
 * @param recipientEmail the contact email captured at booking
 * @param type           the notification type name (CONFIRMATION / RESCHEDULE / CANCELLATION)
 * @param subject        the email subject line
 * @param message        the message body
 */
public record NotificationTriggerRequest(
        UUID appointmentId,
        UUID patientId,
        String recipientEmail,
        String type,
        String subject,
        String message) {
}
