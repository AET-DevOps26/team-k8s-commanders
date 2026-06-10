package com.caredesk.patient.service;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.junit.jupiter.api.Test;
import org.openapitools.model.AppointmentCreate;
import org.openapitools.model.AppointmentRescheduleRequest;
import org.openapitools.model.AppointmentStatus;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AppointmentService}. Backed by a mocked repository
 * so the tests do not need a database.
 */
class AppointmentServiceTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final DoctorSlotRepository doctorSlotRepository = mock(DoctorSlotRepository.class);
    private final AppointmentMapper mapper = new AppointmentMapper();
    private final AppointmentService service = new AppointmentService(repository, doctorSlotRepository, mapper);

    @Test
    void book_createsScheduledAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime when = OffsetDateTime.now().plusDays(2);
        AppointmentCreate request = new AppointmentCreate(patientId, doctorId, when, 30);
        request.setReason("Check-up");
        DoctorSlot slot = slot(doctorId, when, 30, true);
        when(doctorSlotRepository.findAndLockAvailableSlot(eq(doctorId), eq(when), eq(when.plusMinutes(30))))
                .thenReturn(Optional.of(slot));
        when(doctorSlotRepository.save(any(DoctorSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        org.openapitools.model.Appointment created = service.book(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(captor.capture());
        Appointment persisted = captor.getValue();
        assertThat(persisted.getPatientId()).isEqualTo(patientId);
        assertThat(persisted.getDoctorId()).isEqualTo(doctorId);
        assertThat(persisted.getDateTime()).isEqualTo(when);
        assertThat(persisted.getDuration()).isEqualTo(30);
        assertThat(persisted.getReason()).isEqualTo("Check-up");
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(slot.getAvailable()).isFalse();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void book_rejectsUnavailableSlot() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime when = OffsetDateTime.now().plusDays(2);
        AppointmentCreate request = new AppointmentCreate(patientId, doctorId, when, 30);
        when(doctorSlotRepository.findAndLockAvailableSlot(eq(doctorId), eq(when), eq(when.plusMinutes(30))))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(request))
                .isInstanceOf(AppointmentStateConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void book_rejectsPastAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime when = OffsetDateTime.now().minusHours(1);
        AppointmentCreate request = new AppointmentCreate(patientId, doctorId, when, 30);

        assertThatThrownBy(() -> service.book(request))
                .isInstanceOf(AppointmentStateConflictException.class)
                .hasMessageContaining("Past appointment cannot be booked");
        verify(doctorSlotRepository, never()).findAndLockAvailableSlot(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void list_returnsPagedResponse() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED);
        Page<Appointment> page = new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1);
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        PaginatedAppointmentResponse response = service.list(0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage().getTotalElements()).isEqualTo(1);
        assertThat(response.getPage().getTotalPages()).isEqualTo(1);
    }

    @Test
    void getById_returnsAppointment_whenFound() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED);
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));

        org.openapitools.model.Appointment dto = service.getById(a.getId());

        assertThat(dto.getId()).isEqualTo(a.getId());
        assertThat(dto.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void reschedule_updatesDateTimeAndMarksRescheduled() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED);
        OffsetDateTime newWhen = OffsetDateTime.now().plusDays(3);
        DoctorSlot oldSlot = slot(a.getDoctorId(), a.getDateTime(), a.getDuration(), false);
        DoctorSlot newSlot = slot(a.getDoctorId(), newWhen, 45, true);
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));
        when(doctorSlotRepository.findAndLockAvailableSlot(eq(a.getDoctorId()), eq(newWhen), eq(newWhen.plusMinutes(45))))
                .thenReturn(Optional.of(newSlot));
        when(doctorSlotRepository.findSlotByTime(eq(a.getDoctorId()), eq(a.getDateTime()), eq(a.getDateTime().plusMinutes(a.getDuration()))))
                .thenReturn(Optional.of(oldSlot));
        when(doctorSlotRepository.save(any(DoctorSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentRescheduleRequest req = new AppointmentRescheduleRequest(newWhen);
        req.setDuration(45);
        org.openapitools.model.Appointment dto = service.reschedule(a.getId(), req);

        assertThat(dto.getDateTime()).isEqualTo(newWhen);
        assertThat(dto.getDuration()).isEqualTo(45);
        assertThat(dto.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        assertThat(oldSlot.getAvailable()).isTrue();
        assertThat(newSlot.getAvailable()).isFalse();
    }

    @Test
    void reschedule_leavesDurationUntouched_whenOmitted() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED);
        a.setDuration(30);
        OffsetDateTime newWhen = OffsetDateTime.now().plusDays(3);
        DoctorSlot oldSlot = slot(a.getDoctorId(), a.getDateTime(), a.getDuration(), false);
        DoctorSlot newSlot = slot(a.getDoctorId(), newWhen, 30, true);
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));
        when(doctorSlotRepository.findAndLockAvailableSlot(eq(a.getDoctorId()), eq(newWhen), eq(newWhen.plusMinutes(30))))
                .thenReturn(Optional.of(newSlot));
        when(doctorSlotRepository.findSlotByTime(eq(a.getDoctorId()), eq(a.getDateTime()), eq(a.getDateTime().plusMinutes(a.getDuration()))))
                .thenReturn(Optional.of(oldSlot));
        when(doctorSlotRepository.save(any(DoctorSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentRescheduleRequest req = new AppointmentRescheduleRequest(newWhen);
        org.openapitools.model.Appointment dto = service.reschedule(a.getId(), req);

        assertThat(dto.getDuration()).isEqualTo(30);
    }

    @Test
    void reschedule_rejectsCancelledAppointment() {
        Appointment a = appointment(AppointmentStatus.CANCELLED);
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));

        AppointmentRescheduleRequest req = new AppointmentRescheduleRequest(
                OffsetDateTime.now().plusDays(3));
        assertThatThrownBy(() -> service.reschedule(a.getId(), req))
                .isInstanceOf(AppointmentStateConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void reschedule_rejectsPastAppointment() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED, OffsetDateTime.now().minusDays(1));
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));

        AppointmentRescheduleRequest req = new AppointmentRescheduleRequest(
                OffsetDateTime.now().plusDays(2));
        assertThatThrownBy(() -> service.reschedule(a.getId(), req))
                .isInstanceOf(AppointmentStateConflictException.class)
                .hasMessageContaining("Past appointment cannot be rescheduled");
        verify(doctorSlotRepository, never()).findAndLockAvailableSlot(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void cancel_setsStatusToCancelled() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED);
        DoctorSlot slot = slot(a.getDoctorId(), a.getDateTime(), a.getDuration(), false);
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));
        when(doctorSlotRepository.findSlotByTime(eq(a.getDoctorId()), eq(a.getDateTime()), eq(a.getDateTime().plusMinutes(a.getDuration()))))
                .thenReturn(Optional.of(slot));
        when(doctorSlotRepository.save(any(DoctorSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        org.openapitools.model.Appointment dto = service.cancel(a.getId());

        assertThat(dto.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.getAvailable()).isTrue();
    }

    @Test
    void cancel_isIdempotent_whenAlreadyCancelled() {
        Appointment a = appointment(AppointmentStatus.CANCELLED);
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));

        org.openapitools.model.Appointment dto = service.cancel(a.getId());

        assertThat(dto.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        // The already-cancelled row should not be saved again.
        verify(repository, never()).save(any());
    }

    @Test
    void cancel_rejectsPastAppointment() {
        Appointment a = appointment(AppointmentStatus.SCHEDULED, OffsetDateTime.now().minusDays(1));
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.cancel(a.getId()))
                .isInstanceOf(AppointmentStateConflictException.class)
                .hasMessageContaining("Past appointment cannot be cancelled");
        verify(doctorSlotRepository, never()).findSlotByTime(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void cancel_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    private static Appointment appointment(AppointmentStatus status) {
        return appointment(status, OffsetDateTime.now().plusDays(2));
    }

    private static Appointment appointment(AppointmentStatus status, OffsetDateTime dateTime) {
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setPatientId(UUID.randomUUID());
        a.setDoctorId(UUID.randomUUID());
        a.setDateTime(dateTime);
        a.setStatus(status);
        a.setDuration(30);
        return a;
    }

    private static DoctorSlot slot(UUID doctorId, OffsetDateTime startAt, int duration, boolean available) {
        DoctorSlot slot = new DoctorSlot();
        slot.setId(UUID.randomUUID());
        slot.setDoctorId(doctorId);
        slot.setStartAt(startAt);
        slot.setEndAt(startAt.plusMinutes(duration));
        slot.setAvailable(available);
        return slot;
    }
}
