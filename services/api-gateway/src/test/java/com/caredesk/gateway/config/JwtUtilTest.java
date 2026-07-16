package com.caredesk.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-256";

    @Test
    void initRejectsSecretsShorterThan256Bits() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "too-short");

        assertThatThrownBy(jwtUtil::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void parseClaimsReturnsVerifiedIdentity() {
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = Jwts.builder()
                .subject("doctor@example.com")
                .claim("role", "DOCTOR")
                .claim("uid", "11111111-1111-1111-1111-111111111111")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Claims claims = jwtUtil.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("doctor@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("DOCTOR");
    }

    @Test
    void parseClaimsRejectsTokenSignedWithDifferentSecret() {
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = Jwts.builder()
                .subject("attacker@example.com")
                .signWith(Keys.hmacShaKeyFor(
                        "different-secret-key-long-enough-for-hmac-256".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtUtil.parseClaims(token)).isInstanceOf(RuntimeException.class);
    }

    private static JwtUtil configuredJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        jwtUtil.init();
        return jwtUtil;
    }
}
