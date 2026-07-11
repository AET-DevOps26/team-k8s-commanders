package com.caredesk.patient.service;

import org.openapitools.model.UserRole;

import java.util.UUID;

/**
 * The authenticated caller's identity, as derived from the trusted
 * gateway-injected {@code X-User-Id} / {@code X-User-Role} headers.
 *
 * <p>Passed from the controllers into the service layer so per-record
 * ownership rules (a patient may only touch their own appointments, a doctor
 * only theirs) can be enforced where the entities are loaded.
 *
 * @param userId the caller's user id from auth-service
 * @param role   the caller's role
 */
public record Caller(UUID userId, UserRole role) {

    /** @return {@code true} if the caller is a clinic administrator */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /** @return {@code true} if the caller is a doctor */
    public boolean isDoctor() {
        return role == UserRole.DOCTOR;
    }

    /** @return {@code true} if the caller is a patient */
    public boolean isPatient() {
        return role == UserRole.PATIENT;
    }

    /**
     * @param id an owner id to compare against, may be {@code null}
     * @return {@code true} if the caller is that user
     */
    public boolean is(UUID id) {
        return userId != null && userId.equals(id);
    }
}
