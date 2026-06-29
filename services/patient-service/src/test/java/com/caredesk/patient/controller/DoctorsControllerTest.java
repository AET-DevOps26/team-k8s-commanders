package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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
        ScheduleSlot created = new ScheduleSlot(input.getStartAt(), input.getEndAt(), true);
        when(request.getHeader("X-User-Role")).thenReturn("DOCTOR");
        when(request.getHeader("X-User-Id")).thenReturn(doctorId.toString());
        when(doctorService.createScheduleSlot(doctorId, input)).thenReturn(created);

        ResponseEntity<ScheduleSlot> result = controller.createDoctorScheduleSlot(doctorId, input);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(created);
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
}
