package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.RecurringScheduleCreate;
import org.openapitools.model.RecurringScheduleResult;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.Weekday;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorsControllerTest {

    private final DoctorService doctorService = mock(DoctorService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final DoctorsController controller = new DoctorsController(doctorService, request);

    @Test
    void listDoctors_usesDefaultPagination_whenPageAndSizeAreNull() {
        PaginatedUserProfileResponse response = new PaginatedUserProfileResponse(
                List.of(), new PageMeta(0, 20, 0L, 0));
        when(doctorService.listDoctors(isNull(), isNull(), eq(0), eq(20))).thenReturn(response);

        ResponseEntity<PaginatedUserProfileResponse> result = controller.listDoctors(null, null, null, null);

        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void createDoctorScheduleSlot_returnsCreatedForOwnDoctor() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2035-06-08T09:00:00Z");
        ScheduleSlotCreate input = new ScheduleSlotCreate(startAt, startAt.plusMinutes(30));
        ScheduleSlot created = new ScheduleSlot(UUID.randomUUID(), input.getStartAt(), input.getEndAt(), true);
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(doctorId.toString());
        when(doctorService.createScheduleSlot(doctorId, input)).thenReturn(created);

        ResponseEntity<ScheduleSlot> result = controller.createDoctorScheduleSlot(doctorId, input);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(created);
    }

    @Test
    void createDoctorScheduleSlot_verifiesTargetDoctor_whenAdmin() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2035-06-08T09:00:00Z");
        ScheduleSlotCreate input = new ScheduleSlotCreate(startAt, startAt.plusMinutes(30));
        ScheduleSlot created = new ScheduleSlot(UUID.randomUUID(), input.getStartAt(), input.getEndAt(), true);
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        when(doctorService.createScheduleSlot(doctorId, input)).thenReturn(created);

        ResponseEntity<ScheduleSlot> result = controller.createDoctorScheduleSlot(doctorId, input);

        verify(doctorService).verifyDoctorExists(doctorId);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createDoctorScheduleSlot_rejectsOtherDoctor() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2035-06-08T09:00:00Z");
        ScheduleSlotCreate input = new ScheduleSlotCreate(startAt, startAt.plusMinutes(30));
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> controller.createDoctorScheduleSlot(doctorId, input))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void createDoctorRecurringSchedule_returnsCreatedForOwnDoctor() {
        UUID doctorId = UUID.randomUUID();
        RecurringScheduleCreate input = recurringInput();
        RecurringScheduleResult result = new RecurringScheduleResult(List.of(), List.of());
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(doctorId.toString());
        when(doctorService.createRecurringScheduleSlots(doctorId, input)).thenReturn(result);

        ResponseEntity<RecurringScheduleResult> response = controller.createDoctorRecurringSchedule(doctorId, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(result);
        verify(doctorService, never()).verifyDoctorExists(doctorId);
    }

    @Test
    void createDoctorRecurringSchedule_verifiesTargetDoctor_whenAdmin() {
        UUID doctorId = UUID.randomUUID();
        RecurringScheduleCreate input = recurringInput();
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        when(doctorService.createRecurringScheduleSlots(doctorId, input))
                .thenReturn(new RecurringScheduleResult(List.of(), List.of()));

        ResponseEntity<RecurringScheduleResult> response = controller.createDoctorRecurringSchedule(doctorId, input);

        verify(doctorService).verifyDoctorExists(doctorId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createDoctorRecurringSchedule_rejectsOtherDoctor() {
        UUID doctorId = UUID.randomUUID();
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> controller.createDoctorRecurringSchedule(doctorId, recurringInput()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void deleteDoctorScheduleSlot_returnsNoContentForOwnDoctor() {
        UUID doctorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(doctorId.toString());

        ResponseEntity<Void> response = controller.deleteDoctorScheduleSlot(doctorId, slotId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(doctorService).deleteScheduleSlot(doctorId, slotId);
    }

    @Test
    void deleteDoctorScheduleSlot_skipsDoctorVerification_whenAdmin() {
        UUID doctorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

        ResponseEntity<Void> response = controller.deleteDoctorScheduleSlot(doctorId, slotId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(doctorService, never()).verifyDoctorExists(doctorId);
        verify(doctorService).deleteScheduleSlot(doctorId, slotId);
    }

    @Test
    void deleteDoctorScheduleSlot_rejectsOtherDoctor() {
        UUID doctorId = UUID.randomUUID();
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> controller.deleteDoctorScheduleSlot(doctorId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    private static RecurringScheduleCreate recurringInput() {
        return new RecurringScheduleCreate(
                Set.of(Weekday.MONDAY),
                "09:00",
                "12:00",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30,
                LocalDate.of(2035, 1, 1),
                LocalDate.of(2035, 1, 14),
                "Europe/Berlin");
    }
}
