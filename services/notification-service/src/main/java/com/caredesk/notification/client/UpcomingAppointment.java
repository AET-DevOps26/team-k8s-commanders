package com.caredesk.notification.client;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One upcoming appointment as returned by patient-service's internal
 * {@code GET /internal/appointments/upcoming} feed, which the reminder
 * scheduler consumes.
 *
 * <p>The recipient email is captured by patient-service at booking time and
 * carried here, so the reminder scheduler never has to resolve it from
 * auth-service.
 *
 * @param appointmentId  the appointment id
 * @param patientId      the patient's user id
 * @param dateTime       the appointment start time
 * @param recipientEmail the contact email captured at booking, may be {@code null}
 */
public record UpcomingAppointment(
        UUID appointmentId,
        UUID patientId,
        OffsetDateTime dateTime,
        String recipientEmail) {
}
