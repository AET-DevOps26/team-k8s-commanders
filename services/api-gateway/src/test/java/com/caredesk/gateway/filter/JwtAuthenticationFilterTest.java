package com.caredesk.gateway.filter;

import com.caredesk.gateway.config.JwtUtil;
import com.caredesk.gateway.error.GatewayProblemDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final FakeJwtUtil jwtUtil = new FakeJwtUtil();
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            new GatewayProblemDetails(new ObjectMapper()));

    @Test
    void publicRouteRemovesSpoofedIdentityHeaders() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/auth/login")
                .header("X-User-Email", "attacker@example.com")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Id", "spoofed"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Email")).isNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role")).isNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(jwtUtil.lastToken).isNull();
    }

    @Test
    void optionsRequestBypassesAuthentication() {
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/patients").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void protectedRouteWithoutBearerTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/patients"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"title\":\"Unauthorized\"")
                .contains("\"status\":401")
                .contains("\"detail\":\"Authentication is required\"");
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void invalidTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/patients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        jwtUtil.failure = new IllegalArgumentException("bad token");

        filter.filter(exchange, capturingChain(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void validTokenForwardsVerifiedIdentity() {
        Claims claims = Jwts.claims()
                .subject("doctor@example.com")
                .add("role", "DOCTOR")
                .add("uid", "11111111-1111-1111-1111-111111111111")
                .build();
        jwtUtil.claims = claims;
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/patients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid")
                .header("X-User-Role", "ADMIN"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwarded)).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("doctor@example.com");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("DOCTOR");
        assertThat(headers.getFirst("X-User-Id"))
                .isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void tokenWithoutRequiredIdentityClaimsReturnsUnauthorized() {
        Claims claims = Jwts.claims().subject("doctor@example.com").build();
        jwtUtil.claims = claims;
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/patients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer missing-role"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void getOrderRunsBeforeRouteForwarding() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request.build());
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }

    private static GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private static final class FakeJwtUtil extends JwtUtil {
        private Claims claims;
        private RuntimeException failure;
        private String lastToken;

        @Override
        public Claims parseClaims(String token) {
            lastToken = token;
            if (failure != null) {
                throw failure;
            }
            return claims;
        }
    }
}
