package com.caredesk.patient.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

// A single bookable slot in a doctor's schedule. `available` flips to false
// when an appointment is booked into it. Doctor identity is owned by
// auth-service so we only carry the UUID, not the profile.
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

    // Rejects zero-length or backwards time ranges before they hit the DB.
    @PrePersist
    @PreUpdate
    private void validateTimeRange() {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("DoctorSlot endAt must be after startAt");
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorSlot that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
