package com.caredesk.auth.controller;

import com.caredesk.auth.service.DuplicateEmailException;
import com.caredesk.auth.service.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsDomainAndSecurityFailuresToProblemDetails() {
        assertProblem(handler.duplicateEmail(new DuplicateEmailException("duplicate")),
                HttpStatus.CONFLICT, "duplicate");
        assertProblem(handler.notFound(new UserNotFoundException("missing")),
                HttpStatus.NOT_FOUND, "missing");
        assertProblem(handler.unauthorized(new BadCredentialsException("invalid")),
                HttpStatus.UNAUTHORIZED, "invalid");
        assertProblem(handler.forbidden(new AccessDeniedException("denied")),
                HttpStatus.FORBIDDEN, "denied");
        assertProblem(handler.badRequest(new IllegalArgumentException("bad input")),
                HttpStatus.BAD_REQUEST, "bad input");
    }

    private static void assertProblem(
            org.springframework.http.ProblemDetail problem,
            HttpStatus status,
            String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
