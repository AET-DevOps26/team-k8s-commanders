package com.caredesk.patient.service;

/**
 * Application event carrying a notification trigger for a committed appointment
 * change.
 *
 * <p>Published by {@link AppointmentService} within the booking / reschedule /
 * cancel transaction and consumed by {@link AppointmentNotificationListener}
 * only after the transaction commits, so a notification is never dispatched for
 * a change that rolled back.
 *
 * @param request the notification trigger payload
 */
public record AppointmentNotificationEvent(NotificationTriggerRequest request) {
}
