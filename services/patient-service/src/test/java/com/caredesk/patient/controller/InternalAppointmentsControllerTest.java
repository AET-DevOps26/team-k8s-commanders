package com.caredesk.patient.controller;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.model.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAppointmentsControllerTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final InternalAppointmentsController controller = new InternalAppointmentsController(repository);

    @Test
    void upcomingReturnsOnlyDeliverableActiveAppointments() {
        Appointment deliverable = appointment("patient@example.com");
        Appointment missingEmail = appointment(null);
        Appointment blankEmail = appointment("  ");
        when(repository.findByStatusInAndDateTimeBetween(any(), any(), any()))
                .thenReturn(List.of(deliverable, missingEmail, blankEmail));

        List<UpcomingAppointment> result = controller.upcoming(12);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.appointmentId()).isEqualTo(deliverable.getId());
            assertThat(item.recipientEmail()).isEqualTo("patient@example.com");
        });
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<AppointmentStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> until = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findByStatusInAndDateTimeBetween(statuses.capture(), from.capture(), until.capture());
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(
                AppointmentStatus.SCHEDULED, AppointmentStatus.RESCHEDULED);
        assertThat(until.getValue()).isAfter(from.getValue().plusHours(11));
    }

    private static Appointment appointment(String email) {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setDateTime(OffsetDateTime.now().plusHours(2));
        appointment.setPatientEmail(email);
        return appointment;
    }
}
