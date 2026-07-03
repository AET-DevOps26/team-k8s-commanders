package com.caredesk.auth.service;

import com.caredesk.auth.config.JwtUtil;
import com.caredesk.auth.exception.LoginFailedException;
import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String ACCOUNT_DEACTIVATED = "Account deactivated";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthServiceImpl service =
            new AuthServiceImpl(userRepository, new BCryptPasswordEncoder(), authenticationManager, jwtUtil, new UserProfileMapper());

    @Test
    void loginRejectsUnknownUserWithGenericMessage() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> service.login(new LoginRequest("missing@clinic.com", "secret123")))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage(INVALID_CREDENTIALS);
    }

    @Test
    void loginRejectsDeactivatedAccount() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> service.login(new LoginRequest("inactive@clinic.com", "secret123")))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage(ACCOUNT_DEACTIVATED);
    }

    @Test
    void loginRejectsWrongPasswordWithGenericMessage() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> service.login(new LoginRequest("patient@clinic.com", "wrong")))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage(INVALID_CREDENTIALS);
    }

    /**
     * The core anti-enumeration property: unknown email and wrong password must
     * be byte-identical to the caller.
     */
    @Test
    void unknownUserAndWrongPasswordAreIndistinguishable() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found"))
                .thenThrow(new BadCredentialsException("bad credentials"));

        String unknownUserMessage = loginFailureMessage("missing@clinic.com");
        String wrongPasswordMessage = loginFailureMessage("patient@clinic.com");

        org.assertj.core.api.Assertions.assertThat(unknownUserMessage).isEqualTo(wrongPasswordMessage);
    }

    private String loginFailureMessage(String email) {
        try {
            service.login(new LoginRequest(email, "whatever"));
            throw new AssertionError("login should have failed");
        } catch (LoginFailedException ex) {
            return ex.getMessage();
        }
    }

    @Test
    void loginReturnsSessionWhenAuthenticationSucceeds() {
        User user = activeUser("patient@clinic.com", "encoded");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getRole().name()))
                .thenReturn("jwt-token");

        var session = service.login(new LoginRequest("patient@clinic.com", "correct"));

        org.assertj.core.api.Assertions.assertThat(session.getAccessToken()).isEqualTo("jwt-token");
    }

    private static User activeUser(String email, String encodedPassword) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        return user;
    }
}
