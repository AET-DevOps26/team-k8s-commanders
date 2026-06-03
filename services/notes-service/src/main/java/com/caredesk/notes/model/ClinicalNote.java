package com.caredesk.notes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A structured clinical note written by a doctor after a patient visit.
 *
 * <p>Each note is tied to exactly one appointment ({@code appointmentId} is
 * unique) and authored by one doctor. The appointment and doctor identities
 * are owned by other services (patient-service and auth-service) on different
 * databases, so they are referenced by UUID rather than a JPA association.
 */
@Entity
@Table(name = "clinical_notes",
        uniqueConstraints = @UniqueConstraint(name = "uk_clinical_notes_appointment", columnNames = "appointment_id"))
public class ClinicalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The appointment this note documents. One note per appointment. */
    @NotNull
    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    /** The authoring doctor's user id from auth-service. */
    @NotNull
    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    /** Free-text consultation findings and treatment summary. */
    @NotNull
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** Optional structured diagnosis tag for this visit. */
    @Embedded
    private Diagnosis diagnosis;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** @return the generated note id */
    public UUID getId() { return id; }

    /** @param id the note id, typically set by JPA on persist */
    public void setId(UUID id) { this.id = id; }

    /** @return the id of the appointment this note documents */
    public UUID getAppointmentId() { return appointmentId; }

    /** @param appointmentId the id of the appointment this note documents */
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }

    /** @return the authoring doctor's user id from auth-service */
    public UUID getDoctorId() { return doctorId; }

    /** @param doctorId the authoring doctor's user id from auth-service */
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    /** @return the note's free-text content */
    public String getContent() { return content; }

    /** @param content the note's free-text content */
    public void setContent(String content) { this.content = content; }

    /** @return the structured diagnosis tag, or {@code null} */
    public Diagnosis getDiagnosis() { return diagnosis; }

    /** @param diagnosis the structured diagnosis tag */
    public void setDiagnosis(Diagnosis diagnosis) { this.diagnosis = diagnosis; }

    /** @return the time the note was created, with offset */
    public OffsetDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt the time the note was created, with offset */
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * JPA-safe equality based on the primary key.
     *
     * @param o the other object
     * @return {@code true} if the other object is a persisted
     *         {@code ClinicalNote} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClinicalNote that)) return false;
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
