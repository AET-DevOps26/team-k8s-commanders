package com.caredesk.auth.service;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PasswordChangeRequest;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserAccountService service = new UserAccountService(userRepository, passwordEncoder);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUser_updatesOwnProfile() {
        User user = user("patient@example.com", Role.PATIENT);
        authenticate(user);
        UserProfile request = new UserProfile(user.getId(), "CareDesk Patient", "new@example.com", UserRole.PATIENT);
        request.setPhoneNumber("+49 89 123456");
        request.setDateOfBirth(LocalDate.parse("1990-04-12"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateUser(user.getId(), request);

        assertThat(updated.getName()).isEqualTo("CareDesk Patient");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getPhoneNumber()).isEqualTo("+49 89 123456");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.parse("1990-04-12"));
    }

    @Test
    void updateUser_preservesPhoneAndDateOfBirth_whenOmittedFromRequest() {
        User user = user("patient@example.com", Role.PATIENT);
        user.setPhoneNumber("+49 89 123456");
        user.setDateOfBirth(LocalDate.parse("1990-04-12"));
        authenticate(user);
        UserProfile request = new UserProfile(user.getId(), "Updated Name", null, UserRole.PATIENT);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateUser(user.getId(), request);

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getPhoneNumber()).isEqualTo("+49 89 123456");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.parse("1990-04-12"));
    }

    @Test
    void updateUser_rejectsDuplicateEmail() {
        User user = user("patient@example.com", Role.PATIENT);
        User existing = user("existing@example.com", Role.PATIENT);
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));

        UserProfile request = new UserProfile(user.getId(), "CareDesk Patient", "existing@example.com", UserRole.PATIENT);

        assertThatThrownBy(() -> service.updateUser(user.getId(), request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateUser_resolvesCurrentUserFromUserDetailsPrincipal() {
        User user = user("patient@example.com", Role.PATIENT);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()));
        UserProfile request = new UserProfile(user.getId(), "Updated Name", null, UserRole.PATIENT);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateUser(user.getId(), request);

        assertThat(updated.getName()).isEqualTo("Updated Name");
    }

    @Test
    void changePassword_updatesEncodedPassword_whenCurrentPasswordMatches() {
        User user = user("patient@example.com", Role.PATIENT);
        user.setPassword("encoded-current");
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current123", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("next12345")).thenReturn("encoded-next");

        service.changePassword(user.getId(), new PasswordChangeRequest("current123", "next12345"));

        assertThat(user.getPassword()).isEqualTo("encoded-next");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_rejectsInvalidCurrentPassword() {
        User user = user("patient@example.com", Role.PATIENT);
        user.setPassword("encoded-current");
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong123", "encoded-current")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(user.getId(),
                new PasswordChangeRequest("wrong123", "next12345")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        ));
    }

    private static User user(String email, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Patient");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        return user;
    }
}
