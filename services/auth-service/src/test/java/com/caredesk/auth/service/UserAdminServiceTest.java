package com.caredesk.auth.service;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.UserCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.openapitools.model.UserStats;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserAdminService service =
            new UserAdminService(userRepository, passwordEncoder, new UserProfileMapper());

    @Test
    void createUserPersistsRequestedRoleWithEncodedPassword() {
        when(userRepository.existsByEmail("doc@clinic.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserCreate request = new UserCreate("Dr. House", "doc@clinic.com", "secret123", UserRole.DOCTOR);
        request.setSpecialization("Diagnostics");

        UserProfile profile = service.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(profile.getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(profile.getSpecialization()).isEqualTo("Diagnostics");
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@clinic.com")).thenReturn(true);

        UserCreate request = new UserCreate("Jane", "taken@clinic.com", "secret123", UserRole.PATIENT);

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void replaceUserPromotesPatientToDoctor() {
        UUID id = UUID.randomUUID();
        User existing = patient(id, "p@clinic.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile update = new UserProfile(id, "Patient One", "p@clinic.com", UserRole.DOCTOR);

        UserProfile result = service.replaceUser(id, update);

        assertThat(result.getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(existing.getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void replaceUserOnlyResetsPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        User existing = patient(id, "p@clinic.com");
        existing.setPassword("old-hash");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile noPassword = new UserProfile(id, "Patient One", "p@clinic.com", UserRole.PATIENT);
        service.replaceUser(id, noPassword);
        assertThat(existing.getPassword()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(anyString());

        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        UserProfile withPassword = new UserProfile(id, "Patient One", "p@clinic.com", UserRole.PATIENT);
        withPassword.setPassword("new-pass");
        service.replaceUser(id, withPassword);
        assertThat(existing.getPassword()).isEqualTo("new-hash");
    }

    @Test
    void deactivateUserDisablesAccount() {
        UUID id = UUID.randomUUID();
        User existing = patient(id, "p@clinic.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateUser(id);

        assertThat(existing.isEnabled()).isFalse();
    }

    @Test
    void deactivateMissingUserThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateUser(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getStatsAggregatesCountsByRoleAndStatus() {
        when(userRepository.countByRole(Role.PATIENT)).thenReturn(5L);
        when(userRepository.countByRole(Role.DOCTOR)).thenReturn(2L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByEnabled(true)).thenReturn(7L);
        when(userRepository.countByEnabled(false)).thenReturn(1L);

        UserStats stats = service.getStats();

        assertThat(stats.getTotal()).isEqualTo(8L);
        assertThat(stats.getPatients()).isEqualTo(5L);
        assertThat(stats.getDoctors()).isEqualTo(2L);
        assertThat(stats.getAdmins()).isEqualTo(1L);
        assertThat(stats.getActive()).isEqualTo(7L);
        assertThat(stats.getDisabled()).isEqualTo(1L);
    }

    private User patient(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("Patient One");
        user.setEmail(email);
        user.setPassword("hash");
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        return user;
    }
}
