package com.caredesk.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/**
 * Reads the caller's identity from the trusted gateway headers.
 *
 * <p>The API gateway validates the JWT and injects {@code X-User-Id} and
 * {@code X-User-Role}. Both controllers need the pair to scope reads, so the
 * parsing lives here. A missing or malformed value means the request did not
 * carry a usable identity from the gateway and is rejected as 401.
 */
final class GatewayIdentity {

    /** Trusted user id injected by the gateway after it validates the JWT. */
    static final String USER_ID_HEADER = "X-User-Id";

    /** Trusted role injected by the gateway after it validates the JWT. */
    static final String ROLE_HEADER = "X-User-Role";

    private GatewayIdentity() {
    }

    /**
     * @param request the current HTTP request
     * @return the caller's user id from the trusted gateway header
     * @throws ResponseStatusException 401 if the header is missing or malformed
     */
    static UUID userId(HttpServletRequest request) {
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity");
        }
    }

    /**
     * @param request the current HTTP request
     * @return the caller's role from the trusted gateway header
     * @throws ResponseStatusException 401 if the header is missing or unknown
     */
    static UserRole role(HttpServletRequest request) {
        String header = request.getHeader(ROLE_HEADER);
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user role");
        }
        try {
            return UserRole.valueOf(header.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user role");
        }
    }
}
