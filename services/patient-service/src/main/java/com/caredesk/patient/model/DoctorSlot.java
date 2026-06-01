package com.caredesk.patient.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single bookable slot in a doctor's schedule.
 *
 * <p>{@code available} flips to {@code false} when an appointment is booked
 * into the slot. Doctor identity is owned by auth-service, so this entity
 * only carries the UUID and not the profile.
 */
@Entity
@Table(name = "doctor_slots")
public class DoctorSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID doctorId;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime startAt;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime endAt;

    @NotNull
    @Column(nullable = false)
    private Boolean available = true;

    /**
     * Rejects zero-length or backwards time ranges before they hit the
     * database. Runs as a JPA lifecycle callback so it covers both inserts
     * and updates.
     *
     * @throws IllegalArgumentException if {@code endAt} is not strictly after
     *                                  {@code startAt}
     */
    @PrePersist
    @PreUpdate
    private void validateTimeRange() {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("DoctorSlot endAt must be after startAt");
        }
    }

    /** @return the generated slot id */
    public UUID getId() { return id; }

    /** @param id the slot id */
    public void setId(UUID id) { this.id = id; }

    /** @return the doctor's user id from auth-service */
    public UUID getDoctorId() { return doctorId; }

    /** @param doctorId the doctor's user id from auth-service */
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    /** @return the inclusive start time of the slot */
    public OffsetDateTime getStartAt() { return startAt; }

    /** @param startAt the inclusive start time of the slot */
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

    /** @return the exclusive end time of the slot */
    public OffsetDateTime getEndAt() { return endAt; }

    /** @param endAt the exclusive end time of the slot */
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

    /** @return {@code true} when the slot can still be booked */
    public Boolean getAvailable() { return available; }

    /** @param available whether the slot can still be booked */
    public void setAvailable(Boolean available) { this.available = available; }

    /**
     * JPA-safe equality based on the primary key.
     *
     * @param o the other object
     * @return {@code true} if the other object is a persisted
     *         {@code DoctorSlot} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorSlot that)) return false;
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
