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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthServiceImpl service =
            new AuthServiceImpl(userRepository, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(), jwtUtil, new UserProfileMapper());

    @Test
    void loginRejectsUnknownUser() {
        when(userRepository.findByEmail("missing@clinic.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("missing@clinic.com", "secret123")))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage("User does not exist");
    }

    @Test
    void loginRejectsDeactivatedAccount() {
        User user = activeUser("inactive@clinic.com", "encoded");
        user.setEnabled(false);
        when(userRepository.findByEmail("inactive@clinic.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("inactive@clinic.com", "secret123")))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage("Account deactivated");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = activeUser("patient@clinic.com", new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct"));
        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("patient@clinic.com", "wrong")))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage("Wrong password");
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
