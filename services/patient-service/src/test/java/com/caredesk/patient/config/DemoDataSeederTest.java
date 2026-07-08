package com.caredesk.patient.config;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.AppointmentStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final DoctorSlotRepository doctorSlotRepository = mock(DoctorSlotRepository.class);
    private final DemoDataSeeder seeder =
            new DemoDataSeeder(appointmentRepository, doctorSlotRepository);

    @Test
    void seedsDemoAppointmentsSpanningEveryStatusAndSlots() {
        when(appointmentRepository.findById(any())).thenReturn(Optional.empty());
        when(doctorSlotRepository.findSlotByTime(any(), any(), any())).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<Appointment> appointments = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(8)).save(appointments.capture());
        verify(doctorSlotRepository, times(3)).save(any(DoctorSlot.class));

        List<Appointment> saved = appointments.getAllValues();
        // Every appointment status is represented so all dashboards have content.
        assertThat(saved).extracting(Appointment::getStatus).contains(
                AppointmentStatus.SCHEDULED, AppointmentStatus.RESCHEDULED,
                AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED);
        // All appointments belong to the canonical demo doctor.
        assertThat(saved).allSatisfy(a -> assertThat(a.getDoctorId()).isEqualTo(DemoDataSeeder.DOCTOR_ID));
        // Exactly one upcoming appointment falls inside the 24h reminder window.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        assertThat(saved)
                .filteredOn(a -> a.getDateTime().isAfter(now) && a.getDateTime().isBefore(now.plusHours(24)))
                .singleElement()
                .satisfies(a -> assertThat(a.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED));
    }

    @Test
    void isIdempotent_reusingExistingRowsRatherThanDuplicating() {
        when(appointmentRepository.findById(any())).thenReturn(Optional.of(new Appointment()));
        when(doctorSlotRepository.findSlotByTime(any(), any(), any())).thenReturn(Optional.of(new DoctorSlot()));

        seeder.run(null);

        // Same number of saves on a re-run — upserted onto the fixed ids, never duplicated.
        verify(appointmentRepository, times(8)).save(any(Appointment.class));
        verify(doctorSlotRepository, times(3)).save(any(DoctorSlot.class));
    }
}
