package com.caredesk.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Security entry points that delegate 401/403 responses to the MVC exception
 * resolver so they render as RFC 9457 problem details like every other error.
 */
public final class SecurityProblemHandlers {

    private SecurityProblemHandlers() {
    }

    public static AuthenticationEntryPoint authenticationEntryPoint(
            HandlerExceptionResolver exceptionResolver) {
        return (request, response, exception) ->
                exceptionResolver.resolveException(request, response, null,
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authentication is required"));
    }

    public static AccessDeniedHandler accessDeniedHandler(
            HandlerExceptionResolver exceptionResolver) {
        return (request, response, exception) ->
                exceptionResolver.resolveException(request, response, null,
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access is denied"));
    }
}
