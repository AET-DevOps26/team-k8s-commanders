package com.caredesk.notes.controller;

import com.caredesk.notes.service.NotesService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ClinicalNote;
import org.openapitools.model.ClinicalNoteInput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotesControllerTest {

    private final NotesService notesService = mock(NotesService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final NotesController controller = new NotesController(notesService, request);

    @Test
    void getAppointmentNoteReturnsNoContentWhenNoNoteExists() {
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        when(request.getHeader(NotesController.USER_ID_HEADER)).thenReturn(doctorId.toString());
        when(notesService.getByAppointment(appointmentId)).thenReturn(Optional.empty());

        ResponseEntity<ClinicalNote> response = controller.getAppointmentNote(appointmentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getAppointmentNoteReturnsNoteAuthoredByAnotherDoctor() {
        UUID appointmentId = UUID.randomUUID();
        UUID authoringDoctorId = UUID.randomUUID();
        UUID callingDoctorId = UUID.randomUUID();
        com.caredesk.notes.model.ClinicalNote note = new com.caredesk.notes.model.ClinicalNote();
        note.setId(UUID.randomUUID());
        note.setAppointmentId(appointmentId);
        note.setDoctorId(authoringDoctorId);
        note.setContent("Authored by another doctor");
        note.setCreatedAt(OffsetDateTime.now());

        when(request.getHeader(NotesController.USER_ID_HEADER)).thenReturn(callingDoctorId.toString());
        when(notesService.getByAppointment(appointmentId)).thenReturn(Optional.of(note));

        ResponseEntity<ClinicalNote> response = controller.getAppointmentNote(appointmentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEqualTo("Authored by another doctor");
    }

    @Test
    void getAppointmentNoteReturnsExistingNote() {
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        com.caredesk.notes.model.ClinicalNote note = new com.caredesk.notes.model.ClinicalNote();
        note.setId(UUID.randomUUID());
        note.setAppointmentId(appointmentId);
        note.setDoctorId(doctorId);
        note.setContent("Existing note");
        note.setCreatedAt(OffsetDateTime.now());

        when(request.getHeader(NotesController.USER_ID_HEADER)).thenReturn(doctorId.toString());
        when(notesService.getByAppointment(appointmentId)).thenReturn(Optional.of(note));

        ResponseEntity<ClinicalNote> response = controller.getAppointmentNote(appointmentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEqualTo("Existing note");
    }

    @Test
    void upsertRejectsBlankContentBeforeIdentityLookup() {
        ClinicalNoteInput input = noteInput("  ");

        assertThatThrownBy(() -> controller.upsertAppointmentNote(UUID.randomUUID(), input))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(notesService, request);
    }

    @Test
    void upsertRejectsMissingDoctorIdentity() {
        ClinicalNoteInput input = noteInput("Patient stable");

        assertThatThrownBy(() -> controller.upsertAppointmentNote(UUID.randomUUID(), input))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(notesService);
    }

    @Test
    void upsertReturnsCreatedAndAttributesTrustedDoctor() {
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        ClinicalNoteInput input = noteInput("Patient stable");
        com.caredesk.notes.model.ClinicalNote note = new com.caredesk.notes.model.ClinicalNote();
        note.setId(UUID.randomUUID());
        note.setAppointmentId(appointmentId);
        note.setDoctorId(doctorId);
        note.setContent(input.getContent());
        note.setCreatedAt(OffsetDateTime.now());
        when(request.getHeader(NotesController.USER_ID_HEADER)).thenReturn(doctorId.toString());
        when(notesService.upsert(appointmentId, doctorId, input.getContent(), null))
                .thenReturn(new NotesService.UpsertResult(note, true));

        ResponseEntity<ClinicalNote> response = controller.upsertAppointmentNote(appointmentId, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getDoctorId()).isEqualTo(doctorId);
    }

    private static ClinicalNoteInput noteInput(String content) {
        ClinicalNoteInput input = new ClinicalNoteInput();
        input.setContent(content);
        return input;
    }
}
