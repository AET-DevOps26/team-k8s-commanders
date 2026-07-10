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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPatientDataSeederTest {

    private final DoctorSlotRepository doctorSlotRepository = mock(DoctorSlotRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final DefaultPatientDataSeeder seeder = new DefaultPatientDataSeeder(
            doctorSlotRepository, appointmentRepository);

    @Test
    void seedsSlotsAndUpcomingAppointments() {
        when(doctorSlotRepository.findSlotByTime(any(), any(), any())).thenReturn(Optional.empty());
        when(appointmentRepository.findFirstByPatientIdAndDoctorIdAndReason(any(), any(), any()))
                .thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<DoctorSlot> slotCaptor = ArgumentCaptor.forClass(DoctorSlot.class);
        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);

        verify(doctorSlotRepository, times(6)).save(slotCaptor.capture());
        verify(appointmentRepository, times(2)).save(appointmentCaptor.capture());

        List<DoctorSlot> slots = slotCaptor.getAllValues();
        assertThat(slots).filteredOn(slot -> !slot.getAvailable()).hasSize(2);
        assertThat(slots).filteredOn(DoctorSlot::getAvailable).hasSize(4);
        assertThat(slots).allSatisfy(slot -> assertThat(slot.getStartAt()).isBefore(slot.getEndAt()));

        List<Appointment> appointments = appointmentCaptor.getAllValues();
        assertThat(appointments).hasSize(2);
        assertThat(appointments).allSatisfy(appointment -> assertThat(appointment.getId()).isNull());
        assertThat(appointments)
                .filteredOn(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .singleElement()
                .satisfies(appointment -> assertThat(appointment.getDateTime()).isBefore(OffsetDateTime.now()));
        assertThat(appointments)
                .filteredOn(appointment -> appointment.getStatus() == AppointmentStatus.SCHEDULED)
                .singleElement()
                .satisfies(appointment -> assertThat(appointment.getDateTime()).isAfter(OffsetDateTime.now()));
        assertThat(appointments).allSatisfy(appointment -> assertThat(appointment.getReason()).isNotBlank());
    }

    @Test
    void updatesExistingSeededAppointments() {
        when(doctorSlotRepository.findSlotByTime(any(), any(), any())).thenReturn(Optional.empty());
        when(appointmentRepository.findFirstByPatientIdAndDoctorIdAndReason(any(), any(), any()))
                .thenReturn(Optional.of(new Appointment()));

        seeder.run(null);

        verify(doctorSlotRepository, times(6)).save(any(DoctorSlot.class));
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
    }
}
