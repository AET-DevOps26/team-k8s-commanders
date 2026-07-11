package com.caredesk.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import org.openapitools.model.NotificationChannel;
import org.springframework.data.domain.Persistable;

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
public class Notification implements Persistable<UUID> {

    // Ids are application-assigned (not @GeneratedValue) so demo seeding can
    // create rows with fixed ids for idempotency. Persistable + the @PrePersist
    // below keep normal creation working: a new entity with no id gets a random
    // UUID at persist time, and isNew() drives insert-vs-merge.
    @Id
    private UUID id;

    @Transient
    private boolean newEntry = true;

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

    /**
     * Why this notification was sent. Service-internal — not exposed in the API
     * model. Lets the reminder scheduler dedupe reminders per appointment.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private NotificationType type;

    /**
     * The address this notification was delivered to, if any. Service-internal
     * — kept for the record of what was actually sent, not exposed in the API
     * model. Null when no recipient address was known (the record is still
     * stored).
     */
    @Column(name = "recipient_email")
    private String recipientEmail;

    /**
     * Whether the email was actually accepted by the SMTP server. Service-internal
     * — not exposed in the API model. The reminder scheduler dedupes only on
     * <em>delivered</em> reminders, so a failed send is retried on a later scan.
     */
    @Column(nullable = false)
    private boolean delivered;

    /**
     * How many delivery attempts have been made. Service-internal — bounds the
     * reminder scheduler's retries so an undeliverable appointment doesn't
     * re-attempt forever.
     */
    @Column(name = "delivery_attempts", nullable = false)
    private int deliveryAttempts;

    @NotNull
    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    /** @return the notification id */
    @Override
    public UUID getId() { return id; }

    /** @param id the notification id (application-assigned) */
    public void setId(UUID id) { this.id = id; }

    @Override
    public boolean isNew() { return newEntry; }

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

    /** @return why this notification was sent, or {@code null} */
    public NotificationType getType() { return type; }

    /** @param type why this notification was sent */
    public void setType(NotificationType type) { this.type = type; }

    /** @return the address this notification was delivered to, or {@code null} */
    public String getRecipientEmail() { return recipientEmail; }

    /** @param recipientEmail the address this notification was delivered to */
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    /** @return whether the email was accepted by the SMTP server */
    public boolean isDelivered() { return delivered; }

    /** @param delivered whether the email was accepted by the SMTP server */
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    /** @return how many delivery attempts have been made */
    public int getDeliveryAttempts() { return deliveryAttempts; }

    /** @param deliveryAttempts how many delivery attempts have been made */
    public void setDeliveryAttempts(int deliveryAttempts) { this.deliveryAttempts = deliveryAttempts; }

    /** @return the time the notification was sent, with offset */
    public OffsetDateTime getSentAt() { return sentAt; }

    /** @param sentAt the time the notification was sent, with offset */
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }

    /**
     * Assigns a random id for normally-created notifications (seeded rows keep
     * their fixed id) and stamps the sent time on first persist if it has not
     * been set explicitly, satisfying the {@code @NotNull} column constraint.
     */
    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (sentAt == null) {
            sentAt = OffsetDateTime.now();
        }
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        newEntry = false;
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
