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

/**
 * Promotes the trusted gateway headers into the Spring Security context.
 *
 * <p>The API gateway validates the inbound JWT, strips any caller-supplied
 * copies of {@code X-User-Email} and {@code X-User-Role}, then injects fresh
 * values it derived from the verified token. This filter reads those headers
 * and, if both are present and the role is recognised, attaches a
 * {@link UsernamePasswordAuthenticationToken} so downstream Spring Security
 * checks see an authenticated principal. Unknown or empty values leave the
 * request anonymous.
 */
@Component
public class PatientHeaderAuthFilter extends OncePerRequestFilter {

    private static final String EMAIL_HEADER = "X-User-Email";
    private static final String ROLE_HEADER = "X-User-Role";

    /**
     * Allowlist of role values we are willing to honour. Any header value
     * outside this set is treated as no role and the request stays anonymous.
     */
    private static final Set<String> ALLOWED_ROLES = Arrays.stream(UserRole.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * Inspects each request for the trusted gateway headers and, when both
     * are present and the role is in the allowlist, sets a Spring Security
     * authentication on the current thread.
     *
     * @param request     the inbound HTTP request
     * @param response    the outbound HTTP response
     * @param filterChain the remainder of the servlet filter chain
     * @throws ServletException if a downstream filter fails
     * @throws IOException      if I/O fails
     */
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
