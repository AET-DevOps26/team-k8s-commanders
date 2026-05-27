package com.caredesk.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///auth_db",
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac-256"
})
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
