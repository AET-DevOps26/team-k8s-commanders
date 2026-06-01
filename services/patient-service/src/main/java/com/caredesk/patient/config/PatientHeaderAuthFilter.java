package com.caredesk.patient.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openapitools.model.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Reads the trusted X-User-Email and X-User-Role headers set by the API gateway
// after it has validated the JWT. The gateway strips any inbound copies of these
// headers before re-adding them, so anything that arrives here can be trusted
// for as long as the service is only reachable inside the compose network.
@Component
public class PatientHeaderAuthFilter extends OncePerRequestFilter {

    private static final String EMAIL_HEADER = "X-User-Email";
    private static final String ROLE_HEADER = "X-User-Role";

    // Allowlist of role values we are willing to honour. Any header value
    // outside this set is treated as no role and the request stays anonymous.
    private static final Set<String> ALLOWED_ROLES = Arrays.stream(UserRole.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String email = request.getHeader(EMAIL_HEADER);
        String role = request.getHeader(ROLE_HEADER);
        if (email != null && !email.isBlank() && role != null && !role.isBlank()) {
            String normalisedRole = role.toUpperCase(Locale.ROOT);
            if (ALLOWED_ROLES.contains(normalisedRole)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + normalisedRole))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
