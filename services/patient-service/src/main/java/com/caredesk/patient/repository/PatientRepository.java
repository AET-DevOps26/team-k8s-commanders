package com.caredesk.patient.repository;

import com.caredesk.patient.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for {@link Patient}.
 *
 * <p>Patient primary keys are UUIDs that mirror the user id issued by the
 * auth-service, so callers can look up a patient row by the same id they
 * receive in the trusted {@code X-User-Email} flow.
 */
public interface PatientRepository extends JpaRepository<Patient, UUID> {
}
