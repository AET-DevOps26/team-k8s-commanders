package com.caredesk.notes.config;

import com.caredesk.notes.model.ClinicalNote;
import com.caredesk.notes.repository.ClinicalNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    private final ClinicalNoteRepository noteRepository = mock(ClinicalNoteRepository.class);
    private final DemoDataSeeder seeder = new DemoDataSeeder(noteRepository);

    @Test
    void seedsOneDiagnosedNotePerCompletedAppointment() {
        when(noteRepository.findByAppointmentId(any())).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<ClinicalNote> notes = ArgumentCaptor.forClass(ClinicalNote.class);
        verify(noteRepository, times(8)).save(notes.capture());

        List<ClinicalNote> saved = notes.getAllValues();
        assertThat(saved).allSatisfy(note -> {
            assertThat(note.getContent()).isNotBlank();
            assertThat(note.getDoctorId()).isNotNull();
            assertThat(note.getDiagnosis()).isNotNull();
            assertThat(note.getDiagnosis().getCode()).isNotBlank();
        });
        assertThat(saved).extracting(note -> note.getDiagnosis().getCode())
                .contains("I10", "E11.9");
    }

    @Test
    void isIdempotent_reusingExistingNotes() {
        when(noteRepository.findByAppointmentId(any())).thenReturn(Optional.of(new ClinicalNote()));

        seeder.run(null);

        verify(noteRepository, times(8)).save(any(ClinicalNote.class));
    }
}
