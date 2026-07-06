package com.caredesk.notes.controller;

import com.caredesk.notes.service.NoteMapper;
import com.caredesk.notes.service.NotesService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.api.AppointmentsApi;
import org.openapitools.model.ClinicalNote;
import org.openapitools.model.ClinicalNoteInput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Clinical note endpoints for the notes service.
 *
 * <p>The note operations ({@code getAppointmentNote} and
 * {@code upsertAppointmentNote}) live on the OpenAPI {@code Appointments} tag,
 * so they are generated onto {@link AppointmentsApi}. This service only owns
 * the {@code /appointments/{appointmentId}/note} sub-path — the gateway routes
 * that path here and the remaining {@code /appointments/**} routes to the
 * patient-service. The other {@link AppointmentsApi} methods keep their
 * generated 501 defaults and are never reached through the gateway.
 */
@Controller
public class NotesController implements AppointmentsApi {

    /** Trusted user id injected by the gateway after it validates the JWT. */
    static final String USER_ID_HEADER = "X-User-Id";

    private final NotesService notesService;
    private final HttpServletRequest request;

    /**
     * @param notesService clinical note business logic
     * @param request      request-scoped proxy used to read the trusted
     *                     {@code X-User-Id} header for the current call
     */
    public NotesController(NotesService notesService, HttpServletRequest request) {
        this.notesService = notesService;
        this.request = request;
    }

    /**
     * Returns the clinical note for an appointment.
     *
     * <p>Any doctor may read any note, matching the shared clinical-record model
     * used for the patient chart (a doctor reads a patient's full history for
     * continuity of care).
     *
     * @param appointmentId the appointment id
     * @return 200 with the note, or 204 if no note has been written yet
     */
    @Override
    public ResponseEntity<ClinicalNote> getAppointmentNote(UUID appointmentId) {
        com.caredesk.notes.model.ClinicalNote note = notesService.getByAppointment(appointmentId)
                .orElse(null);
        if (note == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(NoteMapper.toModel(note));
    }

    /**
     * Creates or replaces the clinical note for an appointment. Any doctor may
     * write it (shared clinical-record model); the {@code doctorId} recorded on
     * the note is the last writer, taken from the gateway-provided
     * {@code X-User-Id} header, not from the request body.
     *
     * @param appointmentId     the appointment the note documents
     * @param clinicalNoteInput the note content and optional diagnosis
     * @return 201 with the note when created, 200 when an existing note is replaced
     */
    @Override
    public ResponseEntity<ClinicalNote> upsertAppointmentNote(UUID appointmentId, ClinicalNoteInput clinicalNoteInput) {
        String content = clinicalNoteInput.getContent();
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
        }

        UUID doctorId = currentDoctorId();
        NotesService.UpsertResult result = notesService.upsert(
                appointmentId,
                doctorId,
                content,
                NoteMapper.toEntityDiagnosis(clinicalNoteInput.getDiagnosis()));

        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(NoteMapper.toModel(result.note()));
    }

    /**
     * Deletes the clinical note for an appointment. Any doctor may delete it
     * (shared clinical-record model).
     *
     * @param appointmentId the appointment whose note should be deleted
     * @return 204 No Content on success, 404 if no note exists
     */
    @Override
    public ResponseEntity<Void> deleteAppointmentNote(UUID appointmentId) {
        notesService.delete(appointmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reads the authoring doctor's id from the trusted gateway header.
     *
     * @return the doctor's user id
     * @throws ResponseStatusException 401 if the header is missing or malformed,
     *                                 which means the request did not carry a
     *                                 usable identity from the gateway
     */
    private UUID currentDoctorId() {
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity");
        }
    }
}
