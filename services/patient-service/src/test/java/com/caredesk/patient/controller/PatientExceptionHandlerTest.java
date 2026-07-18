package com.caredesk.patient.controller;

import com.caredesk.patient.service.AppointmentNotFoundException;
import com.caredesk.patient.service.AppointmentStateConflictException;
import com.caredesk.patient.service.DoctorNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
        assertProblem(handler.unexpected(new IllegalStateException("database secret")),
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Test
    void rendersFrameworkErrorsAsProblemDetails() throws Exception {
        MockMvc mvc = standaloneSetup(new ThrowingController())
                .setControllerAdvice(handler)
                .build();

        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Missing test resource"));

        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database secret"))));
    }

    @Test
    void validationFailuresIncludeFieldErrors() throws Exception {
        MockMvc mvc = standaloneSetup(new ThrowingController())
                .setControllerAdvice(handler)
                .build();

        mvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[0].field").value("value"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    private static void assertProblem(
            org.springframework.http.ProblemDetail problem,
            HttpStatus status,
            String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getDetail()).isEqualTo(detail);
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing test resource");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("database secret");
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody ValidatedPayload payload) {
        }

        record ValidatedPayload(@NotBlank String value) {
        }
    }
}
