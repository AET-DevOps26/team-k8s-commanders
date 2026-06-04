package com.caredesk.notes.repository;

import com.caredesk.notes.model.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link ClinicalNote}.
 */
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {

    /**
     * Returns the note attached to the given appointment, if one exists.
     * Backs the {@code GET/PUT /appointments/{appointmentId}/note} upsert
     * semantics — there is at most one note per appointment.
     *
     * @param appointmentId the appointment id
     * @return the matching note, or empty if none has been written yet
     */
    Optional<ClinicalNote> findByAppointmentId(UUID appointmentId);

    /**
     * Returns every note authored by the given doctor, in no specific order.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return list of matching notes, possibly empty
     */
    List<ClinicalNote> findByDoctorId(UUID doctorId);
}
