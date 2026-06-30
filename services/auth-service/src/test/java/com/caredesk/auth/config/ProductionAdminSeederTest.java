package com.caredesk.auth.config;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionAdminSeederTest {

    private static final String NAME = "Production Admin";
    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "strong-production-password";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsConfiguredAdminWhenAccountDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        seeder(NAME, EMAIL, PASSWORD).run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User admin = userCaptor.getValue();
        assertThat(admin.getName()).isEqualTo(NAME);
        assertThat(admin.getEmail()).isEqualTo(EMAIL);
        assertThat(admin.getPassword()).isEqualTo("encoded-password");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    void leavesExistingAdminAndPasswordUnchanged() {
        User existingAdmin = new User();
        existingAdmin.setEmail(EMAIL);
        existingAdmin.setPassword("existing-encoded-password");
        existingAdmin.setRole(Role.ADMIN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingAdmin));

        seeder(NAME, EMAIL, PASSWORD).run(null);

        verify(userRepository, never()).save(existingAdmin);
        verifyNoInteractions(passwordEncoder);
        assertThat(existingAdmin.getPassword()).isEqualTo("existing-encoded-password");
    }

    @Test
    void rejectsEmailBelongingToNonAdminAccount() {
        User existingPatient = new User();
        existingPatient.setEmail(EMAIL);
        existingPatient.setRole(Role.PATIENT);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingPatient));

        assertThatThrownBy(() -> seeder(NAME, EMAIL, PASSWORD).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configured production admin email belongs to a non-admin account");

        verify(userRepository, never()).save(existingPatient);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsIncompleteConfigurationBeforeAccessingDatabase() {
        assertThatThrownBy(() -> seeder(NAME, EMAIL, "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Production admin bootstrap is enabled but name, email, or password is missing");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    private ProductionAdminSeeder seeder(String name, String email, String password) {
        return new ProductionAdminSeeder(userRepository, passwordEncoder, name, email, password);
    }
}
