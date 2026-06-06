package com.caredesk.gateway.filter;

import com.caredesk.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

// Validates the JWT once at the edge and forwards the user identity to downstream
// services as trusted headers (X-User-Email, X-User-Role). Downstream services should
// read those headers rather than re-validating the JWT themselves.
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    // Headers we set on the forwarded request. Any inbound value with these names
    // must be stripped first to prevent header spoofing.
    static final String USER_EMAIL_HEADER = "X-User-Email";
    static final String USER_ROLE_HEADER = "X-User-Role";
    static final String USER_ID_HEADER = "X-User-Id";

    // Public paths that bypass JWT validation. /auth is where you obtain the token,
    // so it cannot require one. Paths match the external URL (before StripPrefix).
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/**",
            "/actuator/health"
    );

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Strip any inbound X-User-* headers immediately so a caller cannot forge
        // an identity. We re-add them later only after validating the JWT.
        ServerHttpRequest sanitised = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_EMAIL_HEADER);
                    h.remove(USER_ROLE_HEADER);
                    h.remove(USER_ID_HEADER);
                })
                .build();
        ServerWebExchange sanitisedExchange = exchange.mutate().request(sanitised).build();

        String path = sanitised.getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(sanitisedExchange);
        }

        String authHeader = sanitised.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(sanitisedExchange);
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.parseClaims(token);
        } catch (Exception e) {
            return unauthorized(sanitisedExchange);
        }

        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        if (email == null || email.isBlank() || role == null || role.isBlank()) {
            return unauthorized(sanitisedExchange);
        }

        ServerHttpRequest.Builder mutatedBuilder = sanitised.mutate()
                .header(USER_EMAIL_HEADER, email)
                .header(USER_ROLE_HEADER, role);

        // uid is optional: tokens issued before it was introduced still authenticate
        // on every route. Forward it only when present so downstream services that
        // need the user id (e.g. notes-service) can read it.
        String uid = claims.get("uid", String.class);
        if (uid != null && !uid.isBlank()) {
            mutatedBuilder.header(USER_ID_HEADER, uid);
        }

        ServerHttpRequest mutated = mutatedBuilder.build();

        return chain.filter(sanitisedExchange.mutate().request(mutated).build());
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // Run before route forwarding so headers are injected before the request leaves.
        return -1;
    }
}
