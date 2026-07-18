package com.caredesk.auth.controller;

import com.caredesk.auth.exception.LoginFailedException;
import com.caredesk.auth.exception.ValidationException;
import com.caredesk.auth.service.DuplicateEmailException;
import com.caredesk.auth.service.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsDomainAndSecurityFailuresToProblemDetails() {
        assertProblem(handler.loginFailed(new LoginFailedException("login failed")),
                HttpStatus.UNAUTHORIZED, "login failed");
        assertProblem(handler.validation(new ValidationException("invalid registration")),
                HttpStatus.BAD_REQUEST, "invalid registration");
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
    }
}
