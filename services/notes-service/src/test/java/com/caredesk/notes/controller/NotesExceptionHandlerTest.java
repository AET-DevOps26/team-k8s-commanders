package com.caredesk.notes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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

class NotesExceptionHandlerTest {

    private final NotesExceptionHandler handler = new NotesExceptionHandler();

    @Test
    void mapsExpectedAndUnexpectedFailures() {
        assertThat(handler.forbidden(new AccessDeniedException("denied")).getStatus())
                .isEqualTo(403);
        assertThat(handler.badRequest(new IllegalArgumentException("invalid")).getStatus())
                .isEqualTo(400);
        assertThat(handler.unexpected(new IllegalStateException("secret")).getDetail())
                .isEqualTo("An unexpected error occurred");
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
                .andExpect(jsonPath("$.detail").value("Missing test resource"))
                .andExpect(jsonPath("$.instance").value("/test/not-found"));

        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database secret"))));
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
