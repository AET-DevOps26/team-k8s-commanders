package com.caredesk.auth.config;

import com.caredesk.auth.filter.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Verifies the account-enumeration hardening on the authentication provider
 * wired in {@link SecurityConfig}:
 *
 * <ul>
 *   <li>an unknown email surfaces as {@link BadCredentialsException} — the same
 *       failure as a wrong password, not a distinguishable user-not-found;</li>
 *   <li>a deactivated account with a <em>wrong</em> password also gets the
 *       generic failure (the disabled check runs post-authentication, so
 *       deactivation is never leaked to password-guessing callers);</li>
 *   <li>a deactivated account with the <em>correct</em> password gets
 *       {@link DisabledException}, preserving the helpful message for the
 *       legitimate account owner.</li>
 * </ul>
 */
class SecurityConfigAuthenticationProviderTest {

    private static final String EMAIL = "patient@clinic.com";
    private static final String PASSWORD = "secret123";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class));

    private DaoAuthenticationProvider provider(boolean enabled) {
        UserDetailsService userDetailsService = username -> {
            if (!EMAIL.equals(username)) {
                throw new UsernameNotFoundException("no such user");
            }
            return User.withUsername(EMAIL)
                    .password(encoder.encode(PASSWORD))
                    .roles("PATIENT")
                    .disabled(!enabled)
                    .build();
        };
        return config.authenticationProvider(userDetailsService, encoder);
    }

    @Test
    void unknownEmailFailsExactlyLikeWrongPassword() {
        DaoAuthenticationProvider provider = provider(true);

        Throwable unknownEmail = catchFailure(provider, "missing@clinic.com", PASSWORD);
        Throwable wrongPassword = catchFailure(provider, EMAIL, "wrong-password");

        assertThat(unknownEmail).isInstanceOf(BadCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(BadCredentialsException.class);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    @Test
    void deactivatedAccountWithWrongPasswordGetsGenericFailure() {
        DaoAuthenticationProvider provider = provider(false);

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void deactivatedAccountWithCorrectPasswordIsToldItIsDeactivated() {
        DaoAuthenticationProvider provider = provider(false);

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, PASSWORD)))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void activeAccountWithCorrectPasswordAuthenticates() {
        DaoAuthenticationProvider provider = provider(true);

        var authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, PASSWORD));

        assertThat(authentication.isAuthenticated()).isTrue();
    }

    private static Throwable catchFailure(DaoAuthenticationProvider provider, String email, String password) {
        try {
            provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
            throw new AssertionError("authentication should have failed");
        } catch (RuntimeException ex) {
            return ex;
        }
    }
}
