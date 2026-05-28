package com.caredesk.auth.config;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUserSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DefaultUserSeeder seeder = new DefaultUserSeeder(userRepository, passwordEncoder);

    @Test
    void createsMissingDefaultUsers() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));

        seeder.run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());

        List<User> users = userCaptor.getAllValues();
        assertThat(users)
                .extracting(User::getName, User::getEmail, User::getPassword, User::getRole)
                .containsExactly(
                        tuple("Patient", "patient@patient.com", "encoded-patient123", Role.PATIENT),
                        tuple("Doctor", "doctor@doctor.com", "encoded-doctor123", Role.DOCTOR),
                        tuple("Admin", "admin@admin.com", "encoded-admin123", Role.ADMIN)
                );
    }

    @Test
    void leavesExistingDefaultUsersUnchanged() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        seeder.run(null);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
