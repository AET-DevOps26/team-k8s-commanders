package com.caredesk.patient.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Patient-specific data that lives alongside the appointment scheduling
 * domain.
 *
 * <p>The patient's core identity (email, name, role, password hash) is owned
 * by the auth-service. This entity references the same UUID and only stores
 * fields that are relevant to the scheduling and clinical context.
 */
@Entity
@Table(name = "patients")
public class Patient {

    /**
     * Mirrors the user id from auth-service. No FK across service boundaries
     * is enforced. The join happens at the application layer.
     */
    @Id
    @NotNull
    private UUID id;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    /**
     * @return the patient id, equal to the auth-service user id
     */
    public UUID getId() { return id; }

    /**
     * @param id the patient id, equal to the auth-service user id
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * @return the patient's date of birth, or {@code null} if unknown
     */
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    /**
     * @param dateOfBirth the patient's date of birth
     */
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    /**
     * @return the patient's phone number, or {@code null} if not provided
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * @param phoneNumber the patient's phone number
     */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /**
     * JPA-safe equality based on the primary key. Two patients are equal only
     * when both have been persisted and share the same {@code id}, so
     * transient instances never collide in {@link java.util.Set} or
     * {@link java.util.Map}.
     *
     * @param o the other object
     * @return {@code true} if the other object is a persisted {@code Patient}
     *         with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient that)) return false;
        return id != null && id.equals(that.id);
    }

    /**
     * Returns a hash code that is constant per class. This matches the
     * recommended pattern for JPA entities where the id can be {@code null}
     * until the entity is persisted.
     *
     * @return a constant per-class hash code
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
