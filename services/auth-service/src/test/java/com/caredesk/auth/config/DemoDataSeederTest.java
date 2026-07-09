package com.caredesk.auth.config;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DefaultUserSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DefaultUserSeeder seeder = new DefaultUserSeeder(userRepository, passwordEncoder);

    @Test
    void createsMissingDefaultUsers() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
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
        assertThat(users)
                .extracting(User::getPhoneNumber, User::getDateOfBirth)
                .doesNotContain(tuple(null, null));
        assertThat(users.get(1).getSpecialization()).isEqualTo("General Medicine");
        assertThat(users.get(1).getLicenseNumber()).isEqualTo("DE-CARE-1001");
        assertThat(users.get(1).getClinicId()).isNotNull();
        assertThat(users).allMatch(User::isNew);
    }

    @Test
    void enrichesExistingDefaultUsersWithoutChangingPasswords() {
        User existingPatient = new User();
        existingPatient.setEmail("patient@patient.com");
        existingPatient.setPassword("existing-password");

        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail("patient@patient.com")).thenReturn(Optional.of(existingPatient));
        when(userRepository.findByEmail("doctor@doctor.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));

        seeder.run(null);

        verify(userRepository, times(3)).save(any(User.class));
        verify(passwordEncoder, times(2)).encode(anyString());
        assertThat(existingPatient.getName()).isEqualTo("Patient");
        assertThat(existingPatient.getPhoneNumber()).isEqualTo("+49 89 123456");
        assertThat(existingPatient.getPassword()).isEqualTo("existing-password");
    }
}
