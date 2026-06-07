package com.caredesk.notes.service;

import com.caredesk.notes.model.ClinicalNote;
import com.caredesk.notes.model.Diagnosis;
import com.caredesk.notes.repository.ClinicalNoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for clinical notes.
 *
 * <p>There is at most one note per appointment, so writes are an upsert keyed
 * on {@code appointmentId}: the first write creates the note, later writes
 * replace its content, diagnosis and author.
 */
@Service
public class NotesService {

    private final ClinicalNoteRepository repository;

    /**
     * @param repository Spring Data repository for {@link ClinicalNote}
     */
    public NotesService(ClinicalNoteRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the note attached to the given appointment, if one exists.
     *
     * @param appointmentId the appointment id
     * @return the note, or empty if none has been written yet
     */
    @Transactional(readOnly = true)
    public Optional<ClinicalNote> getByAppointment(UUID appointmentId) {
        return repository.findByAppointmentId(appointmentId);
    }

    /**
     * Creates or replaces the note for an appointment.
     *
     * <p>On create, {@code createdAt} is stamped with the current time and a new
     * row is inserted. On replace, the existing row's id, appointment and
     * creation time are preserved while content, diagnosis and author are
     * overwritten — the note reflects whoever last wrote it.
     *
     * @param appointmentId the appointment the note documents
     * @param doctorId      the authoring doctor's user id (from the gateway)
     * @param content       the note's free-text content
     * @param diagnosis     optional structured diagnosis, may be {@code null}
     * @return the saved note plus whether it was newly created
     */
    @Transactional
    public UpsertResult upsert(UUID appointmentId, UUID doctorId, String content, Diagnosis diagnosis) {
        Optional<ClinicalNote> existing = repository.findByAppointmentId(appointmentId);
        boolean created = existing.isEmpty();

        ClinicalNote note = existing.orElseGet(ClinicalNote::new);
        if (created) {
            note.setAppointmentId(appointmentId);
            note.setCreatedAt(OffsetDateTime.now());
        } else if (!note.getDoctorId().equals(doctorId)) {
            throw new AccessDeniedException("Not your appointment");
        }
        note.setDoctorId(doctorId);
        note.setContent(content);
        note.setDiagnosis(diagnosis);

        return new UpsertResult(repository.save(note), created);
    }

    /**
     * Deletes the note for an appointment. Only the authoring doctor may delete it.
     *
     * @param appointmentId the appointment whose note should be deleted
     * @param doctorId      the caller's user id (from the gateway)
     * @throws AccessDeniedException   if the caller is not the note's author
     * @throws jakarta.persistence.EntityNotFoundException if no note exists
     */
    @Transactional
    public void delete(UUID appointmentId, UUID doctorId) {
        ClinicalNote note = repository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No clinical note exists for appointment " + appointmentId));
        if (!note.getDoctorId().equals(doctorId)) {
            throw new AccessDeniedException("Not your appointment");
        }
        repository.delete(note);
    }

    /**
     * Outcome of an upsert: the persisted note and whether it was created
     * (201) versus replaced (200).
     *
     * @param note    the saved note
     * @param created {@code true} if a new note was inserted
     */
    public record UpsertResult(ClinicalNote note, boolean created) {
    }
}
