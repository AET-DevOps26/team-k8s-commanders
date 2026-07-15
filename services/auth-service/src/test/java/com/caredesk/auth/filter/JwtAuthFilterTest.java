package com.caredesk.auth.filter;

import com.caredesk.auth.config.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesEmailAndRole() throws Exception {
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = jwtUtil.generateToken("user-id", "doctor@example.com", "DOCTOR");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthFilter(jwtUtil).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("doctor@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_DOCTOR");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void invalidTokenLeavesRequestAnonymousAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthFilter(configuredJwtUtil())
                .doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void tokenWithoutRoleStaysAnonymous() throws Exception {
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = jwtUtil.generateToken("user-id", "doctor@example.com", null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        new JwtAuthFilter(jwtUtil)
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static JwtUtil configuredJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil, "secret", "test-secret-key-that-is-long-enough-for-hmac-256");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60_000L);
        return jwtUtil;
    }
}
