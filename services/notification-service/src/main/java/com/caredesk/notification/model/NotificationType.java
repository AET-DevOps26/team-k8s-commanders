package com.caredesk.notification.model;

/**
 * Internal classification of why a notification was sent.
 *
 * <p>This is a service-internal concept and is deliberately not part of the
 * OpenAPI contract (the public {@code Notification} model has no type field).
 * Its main job is to let the reminder scheduler tell a reminder apart from a
 * booking confirmation for the same appointment, so a reminder is sent at most
 * once regardless of how many confirmations an appointment already has.
 */
public enum NotificationType {

    /** Sent when an appointment is first booked. */
    CONFIRMATION,

    /** Sent when an existing appointment is moved to a new time. */
    RESCHEDULE,

    /** Sent when an appointment is cancelled. */
    CANCELLATION,

    /** Sent by the scheduler shortly before an upcoming appointment. */
    REMINDER,

    /** Anything created directly via the public API with no specific trigger. */
    GENERIC
}
