package com.caredesk.patient.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.openapitools.model.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A booked appointment between a patient and a doctor.
 *
 * <p>Patient and doctor are referenced by UUID rather than a JPA association
 * because both identities are owned by the auth-service, on a different
 * database.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID patientId;

    @NotNull
    @Column(nullable = false)
    private UUID doctorId;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime dateTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    /** Duration in minutes. Must be greater than zero. */
    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer duration;

    private String reason;

    /**
     * Contact email captured at booking time (from the gateway-injected
     * {@code X-User-Email} of the booking patient). Stored so the notification
     * service can deliver confirmations and reminders without resolving the
     * address from auth-service. Service-internal — not part of the API model.
     */
    @Column(name = "patient_email")
    private String patientEmail;

    /** @return the generated appointment id */
    public UUID getId() { return id; }

    /** @param id the appointment id, typically set by JPA on persist */
    public void setId(UUID id) { this.id = id; }

    /** @return the patient's user id from auth-service */
    public UUID getPatientId() { return patientId; }

    /** @param patientId the patient's user id from auth-service */
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    /** @return the doctor's user id from auth-service */
    public UUID getDoctorId() { return doctorId; }

    /** @param doctorId the doctor's user id from auth-service */
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    /** @return the appointment start time with offset */
    public OffsetDateTime getDateTime() { return dateTime; }

    /** @param dateTime the appointment start time with offset */
    public void setDateTime(OffsetDateTime dateTime) { this.dateTime = dateTime; }

    /** @return the current appointment status */
    public AppointmentStatus getStatus() { return status; }

    /** @param status the new appointment status */
    public void setStatus(AppointmentStatus status) { this.status = status; }

    /** @return the appointment duration in minutes */
    public Integer getDuration() { return duration; }

    /** @param duration the appointment duration in minutes, must be positive */
    public void setDuration(Integer duration) { this.duration = duration; }

    /** @return the free-text reason for the appointment, or {@code null} */
    public String getReason() { return reason; }

    /** @param reason a free-text reason for the appointment */
    public void setReason(String reason) { this.reason = reason; }

    /** @return the contact email captured at booking, or {@code null} */
    public String getPatientEmail() { return patientEmail; }

    /** @param patientEmail the contact email captured at booking */
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    /**
     * JPA-safe equality based on the primary key.
     *
     * @param o the other object
     * @return {@code true} if the other object is a persisted
     *         {@code Appointment} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
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
