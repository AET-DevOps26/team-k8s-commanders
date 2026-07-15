package com.caredesk.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsLoginFailuresToUnauthorized() {
        ProblemDetail problem = handler.handleLoginFailed(new LoginFailedException("Invalid credentials"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void mapsValidationFailuresToBadRequest() {
        ProblemDetail problem = handler.handleValidation(new ValidationException("Email already in use"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Email already in use");
    }
}
