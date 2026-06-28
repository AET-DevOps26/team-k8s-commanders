package com.caredesk.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.openapitools.model.NotificationChannel;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An automated message recorded against a patient and/or an appointment.
 *
 * <p>The patient and appointment identities are owned by other services
 * (auth-service and patient-service) on different databases, so they are
 * referenced by UUID rather than a JPA association. Both are optional: a
 * notification may target a patient directly (e.g. an account message) or be
 * tied to a specific appointment (confirmations and reminders).
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The appointment this notification refers to, if any. */
    @Column(name = "appointment_id")
    private UUID appointmentId;

    /** The recipient patient's user id from auth-service, if known. */
    @Column(name = "patient_id")
    private UUID patientId;

    /** Human-readable message body as delivered to the recipient. */
    @NotNull
    @Column(nullable = false, columnDefinition = "text")
    private String message;

    /** Delivery channel (EMAIL, SMS, PUSH). */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @NotNull
    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    /** @return the generated notification id */
    public UUID getId() { return id; }

    /** @param id the notification id, typically set by JPA on persist */
    public void setId(UUID id) { this.id = id; }

    /** @return the id of the appointment this notification refers to, or {@code null} */
    public UUID getAppointmentId() { return appointmentId; }

    /** @param appointmentId the id of the appointment this notification refers to */
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }

    /** @return the recipient patient's user id, or {@code null} */
    public UUID getPatientId() { return patientId; }

    /** @param patientId the recipient patient's user id */
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    /** @return the message body */
    public String getMessage() { return message; }

    /** @param message the message body */
    public void setMessage(String message) { this.message = message; }

    /** @return the delivery channel */
    public NotificationChannel getChannel() { return channel; }

    /** @param channel the delivery channel */
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    /** @return the time the notification was sent, with offset */
    public OffsetDateTime getSentAt() { return sentAt; }

    /** @param sentAt the time the notification was sent, with offset */
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }

    /**
     * Stamps the sent time on first persist if it has not been set
     * explicitly, satisfying the {@code @NotNull} / non-null column constraint.
     */
    @PrePersist
    void prePersist() {
        if (sentAt == null) {
            sentAt = OffsetDateTime.now();
        }
    }

    /**
     * JPA-safe equality based on the primary key.
     *
     * @param o the other object
     * @return {@code true} if the other object is a persisted
     *         {@code Notification} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return id != null && id.equals(that.id);
    }

    /**
     * @return a constant per-class hash code, matching the recommended JPA
     *         pattern
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
