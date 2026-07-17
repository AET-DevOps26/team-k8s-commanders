package com.caredesk.common.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityProblemHandlersTest {

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void authenticationEntryPointResolvesUnauthorized() throws Exception {
        SecurityProblemHandlers.authenticationEntryPoint(resolver)
                .commence(request, response, new BadCredentialsException("bad"));

        assertThat(resolvedException().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resolvedException().getReason()).isEqualTo("Authentication is required");
    }

    @Test
    void accessDeniedHandlerResolvesForbidden() throws Exception {
        SecurityProblemHandlers.accessDeniedHandler(resolver)
                .handle(request, response, new AccessDeniedException("denied"));

        assertThat(resolvedException().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resolvedException().getReason()).isEqualTo("Access is denied");
    }

    private ResponseStatusException resolvedException() {
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ResponseStatusException.class);
        return (ResponseStatusException) captor.getValue();
    }
}
