package com.caredesk.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Verifies JWTs issued by auth-service. The signing secret is shared across services
// via the JWT_SECRET env var.
@Component
public class JwtUtil {

    // HMAC-SHA256 requires at least 256 bits of key material.
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    // Derived once at startup; the key derivation is deterministic so we cache it.
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) for HMAC-SHA256. "
                            + "Current length: " + bytes.length
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
