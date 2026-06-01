package com.caredesk.patient.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

// Patient-specific data that lives alongside the appointment scheduling domain.
// The patient's core identity (email, name, role, password hash) is owned by
// the auth-service. We reference the same UUID and only store fields that are
// relevant to the scheduling/clinical context.
@Entity
@Table(name = "patients")
public class Patient {

    // Mirrors the user id from auth-service. No FK across service boundaries;
    // the join happens at the application layer.
    @Id
    @NotNull
    private UUID id;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    // JPA-safe equality based on primary key. Returns false until the entity
    // has an id, so transient instances do not collide in Sets or Maps.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
