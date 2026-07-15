package com.caredesk.patient.controller;

import com.caredesk.patient.service.AppointmentNotFoundException;
import com.caredesk.patient.service.AppointmentStateConflictException;
import com.caredesk.patient.service.DoctorNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PatientExceptionHandlerTest {

    private final PatientExceptionHandler handler = new PatientExceptionHandler();

    @Test
    void mapsBookingFailuresToProblemDetails() {
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        assertProblem(handler.notFound(new AppointmentNotFoundException(appointmentId)),
                HttpStatus.NOT_FOUND, "Appointment not found: " + appointmentId);
        assertProblem(handler.notFound(new DoctorNotFoundException(doctorId)),
                HttpStatus.NOT_FOUND, "Doctor not found: " + doctorId);
        assertProblem(handler.forbidden(new AccessDeniedException("not participant")),
                HttpStatus.FORBIDDEN, "not participant");
        assertProblem(handler.conflict(new AppointmentStateConflictException("slot taken")),
                HttpStatus.CONFLICT, "slot taken");
        assertProblem(handler.badRequest(new IllegalArgumentException("invalid duration")),
                HttpStatus.BAD_REQUEST, "invalid duration");
    }

    private static void assertProblem(
            org.springframework.http.ProblemDetail problem,
            HttpStatus status,
            String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
