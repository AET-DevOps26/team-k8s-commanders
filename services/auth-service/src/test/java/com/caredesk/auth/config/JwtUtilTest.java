package com.caredesk.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-256";

    @Test
    void generatedTokenRoundTripsIdentityClaims() {
        JwtUtil jwtUtil = configuredJwtUtil(60_000);

        String token = jwtUtil.generateToken(
                "11111111-1111-1111-1111-111111111111", "doctor@example.com", "DOCTOR");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("doctor@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("DOCTOR");
    }

    @Test
    void tokenSignedWithAnotherSecretIsInvalid() {
        String token = configuredJwtUtil(60_000)
                .generateToken("user-id", "doctor@example.com", "DOCTOR");

        JwtUtil verifier = configuredJwtUtil(60_000);
        ReflectionTestUtils.setField(
                verifier, "secret", "different-secret-key-long-enough-for-hmac-256");

        assertThat(verifier.isTokenValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtUtil jwtUtil = configuredJwtUtil(-1_000);
        String token = jwtUtil.generateToken("user-id", "doctor@example.com", "DOCTOR");

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    static JwtUtil configuredJwtUtil(long expirationMs) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", expirationMs);
        return jwtUtil;
    }
}
