package com.caredesk.patient.controller;

import com.caredesk.patient.service.AppointmentService;
import com.caredesk.patient.service.Caller;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.openapitools.model.Appointment;
import org.openapitools.model.AppointmentCreate;
import org.openapitools.model.AppointmentRescheduleRequest;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppointmentsControllerTest {

    private final AppointmentService appointmentService = mock(AppointmentService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final AppointmentsController controller = new AppointmentsController(appointmentService, request);

    @Test
    void bookUsesTrustedCallerIdentityAndReturnsCreated() {
        UUID userId = callerIs(UserRole.PATIENT);
        AppointmentCreate input = mock(AppointmentCreate.class);
        Appointment created = mock(Appointment.class);
        when(appointmentService.book(input, new Caller(userId, UserRole.PATIENT))).thenReturn(created);

        var response = controller.bookAppointment(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    void listUsesPaginationAndDoctorIdentity() {
        UUID userId = callerIs(UserRole.DOCTOR);
        PaginatedAppointmentResponse page = mock(PaginatedAppointmentResponse.class);
        when(appointmentService.list(2, 15, new Caller(userId, UserRole.DOCTOR))).thenReturn(page);

        assertThat(controller.listAppointments(2, 15).getBody()).isSameAs(page);
    }

    @Test
    void getRescheduleAndCancelDelegateWithSameCaller() {
        UUID userId = callerIs(UserRole.ADMIN);
        UUID appointmentId = UUID.randomUUID();
        Caller caller = new Caller(userId, UserRole.ADMIN);
        AppointmentRescheduleRequest reschedule = mock(AppointmentRescheduleRequest.class);

        controller.getAppointmentById(appointmentId);
        controller.rescheduleAppointment(appointmentId, reschedule);
        controller.cancelAppointment(appointmentId);

        verify(appointmentService).getById(appointmentId, caller);
        verify(appointmentService).reschedule(appointmentId, reschedule, caller);
        verify(appointmentService).cancel(appointmentId, caller);
    }

    @Test
    void malformedIdentityIsRejectedBeforeServiceCall() {
        when(request.getHeader("X-User-Id")).thenReturn("not-a-uuid");
        when(request.getHeader("X-User-Role")).thenReturn("PATIENT");

        assertThatThrownBy(() -> controller.getAppointmentById(UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(appointmentService);
    }

    private UUID callerIs(UserRole role) {
        UUID userId = UUID.randomUUID();
        when(request.getHeader("X-User-Id")).thenReturn(userId.toString());
        when(request.getHeader("X-User-Role")).thenReturn(role.name());
        return userId;
    }
}
