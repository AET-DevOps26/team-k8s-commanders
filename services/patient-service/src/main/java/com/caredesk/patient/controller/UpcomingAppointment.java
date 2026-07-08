package com.caredesk.patient.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One item in the internal upcoming-appointments feed consumed by
 * notification-service's reminder scheduler. Carries the contact email captured
 * at booking so the scheduler need not resolve it from auth-service.
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
