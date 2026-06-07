package com.caredesk.notes.service;

import com.caredesk.notes.model.ClinicalNote;
import com.caredesk.notes.model.Diagnosis;
import com.caredesk.notes.repository.ClinicalNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotesService} upsert and lookup semantics.
 */
@ExtendWith(MockitoExtension.class)
class NotesServiceTest {

    @Mock
    private ClinicalNoteRepository repository;

    @InjectMocks
    private NotesService notesService;

    private final UUID appointmentId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    @Test
    void upsertInsertsNewNoteWhenNoneExists() {
        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalNote.class))).thenAnswer(inv -> inv.getArgument(0));

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setCode("I10");
        diagnosis.setDescription("Hypertension");

        NotesService.UpsertResult result =
                notesService.upsert(appointmentId, doctorId, "First visit", diagnosis);

        assertThat(result.created()).isTrue();
        ClinicalNote saved = result.note();
        assertThat(saved.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(saved.getDoctorId()).isEqualTo(doctorId);
        assertThat(saved.getContent()).isEqualTo("First visit");
        assertThat(saved.getDiagnosis()).isEqualTo(diagnosis);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void upsertReplacesExistingNoteAndPreservesIdentityAndCreatedAt() {
        UUID existingId = UUID.randomUUID();
        OffsetDateTime originalCreatedAt = OffsetDateTime.now().minusDays(3);

        ClinicalNote existing = new ClinicalNote();
        existing.setId(existingId);
        existing.setAppointmentId(appointmentId);
        existing.setDoctorId(doctorId); // same doctor updates their own note
        existing.setContent("Old content");
        existing.setCreatedAt(originalCreatedAt);

        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(existing));
        when(repository.save(any(ClinicalNote.class))).thenAnswer(inv -> inv.getArgument(0));

        NotesService.UpsertResult result =
                notesService.upsert(appointmentId, doctorId, "Updated content", null);

        assertThat(result.created()).isFalse();
        ClinicalNote saved = result.note();
        // Identity and creation time are preserved across a replace.
        assertThat(saved.getId()).isEqualTo(existingId);
        assertThat(saved.getCreatedAt()).isEqualTo(originalCreatedAt);
        // Content, author and diagnosis reflect the latest write.
        assertThat(saved.getContent()).isEqualTo("Updated content");
        assertThat(saved.getDoctorId()).isEqualTo(doctorId);
        assertThat(saved.getDiagnosis()).isNull();
    }

    @Test
    void deleteRemovesNoteWhenCallerIsAuthor() {
        ClinicalNote existing = new ClinicalNote();
        existing.setAppointmentId(appointmentId);
        existing.setDoctorId(doctorId);
        existing.setContent("Note content");
        existing.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(existing));

        notesService.delete(appointmentId, doctorId);

        verify(repository).delete(existing);
    }

    @Test
    void deleteThrowsWhenCallerIsNotNoteAuthor() {
        UUID otherDoctorId = UUID.randomUUID();

        ClinicalNote existing = new ClinicalNote();
        existing.setAppointmentId(appointmentId);
        existing.setDoctorId(otherDoctorId);
        existing.setContent("Note content");
        existing.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> notesService.delete(appointmentId, doctorId))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteThrowsNotFoundWhenNoNoteExists() {
        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notesService.delete(appointmentId, doctorId))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void upsertThrowsWhenCallerIsNotNoteAuthor() {
        UUID otherDoctorId = UUID.randomUUID();

        ClinicalNote existing = new ClinicalNote();
        existing.setAppointmentId(appointmentId);
        existing.setDoctorId(otherDoctorId);
        existing.setContent("Original content");
        existing.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> notesService.upsert(appointmentId, doctorId, "New content", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByAppointmentReturnsEmptyWhenAbsent() {
        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThat(notesService.getByAppointment(appointmentId)).isEmpty();
    }

    @Test
    void getByAppointmentReturnsNoteWhenPresent() {
        ClinicalNote note = new ClinicalNote();
        note.setAppointmentId(appointmentId);
        when(repository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(note));

        assertThat(notesService.getByAppointment(appointmentId)).containsSame(note);
    }
}
