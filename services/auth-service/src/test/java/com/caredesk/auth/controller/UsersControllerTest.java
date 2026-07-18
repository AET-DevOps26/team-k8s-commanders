package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserAccountService;
import com.caredesk.auth.service.UserAdminService;
import com.caredesk.auth.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.PasswordChangeRequest;
import org.openapitools.model.UserCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserStats;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersControllerTest {

    private final UserAccountService userAccountService = mock(UserAccountService.class);
    private final UserService userService = mock(UserService.class);
    private final UserAdminService userAdminService = mock(UserAdminService.class);
    private final UsersController controller =
            new UsersController(userAccountService, userService, userAdminService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsers_usesAccountServiceForDoctors() {
        PaginatedUserProfileResponse response = mock(PaginatedUserProfileResponse.class);
        authenticate("ROLE_DOCTOR");
        when(userAccountService.listUsers(0, 100)).thenReturn(response);

        assertThat(controller.listUsers(0, 100).getBody()).isSameAs(response);

        verify(userAccountService).listUsers(0, 100);
        verify(userAdminService, never()).listUsers(0, 100);
    }

    @Test
    void listUsers_usesAdminServiceForAdmins() {
        PaginatedUserProfileResponse response = mock(PaginatedUserProfileResponse.class);
        authenticate("ROLE_ADMIN");
        when(userAdminService.listUsers(0, 100)).thenReturn(response);

        assertThat(controller.listUsers(0, 100).getBody()).isSameAs(response);

        verify(userAdminService).listUsers(0, 100);
        verify(userAccountService, never()).listUsers(0, 100);
    }

    @Test
    void unauthenticatedInternalLookupUsesReadOnlyUserService() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = mock(UserProfile.class);
        when(userService.findById(userId)).thenReturn(profile);

        assertThat(controller.getUserById(userId).getBody()).isSameAs(profile);
        verify(userService).findById(userId);
        verify(userAccountService, never()).getUser(userId);
    }

    @Test
    void unauthenticatedInternalLookupReturnsNotFoundForUnknownUser() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> controller.getUserById(userId))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void authenticatedLookupAndSelfServiceUseAccountService() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = mock(UserProfile.class);
        PasswordChangeRequest password = mock(PasswordChangeRequest.class);
        authenticate("ROLE_DOCTOR");
        when(userAccountService.getUser(userId)).thenReturn(profile);
        when(userAccountService.updateUser(userId, profile)).thenReturn(profile);

        assertThat(controller.getUserById(userId).getBody()).isSameAs(profile);
        assertThat(controller.replaceUser(userId, profile).getBody()).isSameAs(profile);
        assertThat(controller.changeUserPassword(userId, password).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(userAccountService).changePassword(userId, password);
    }

    @Test
    void adminMutationsDelegateAndReturnContractStatuses() {
        UUID userId = UUID.randomUUID();
        UserCreate input = mock(UserCreate.class);
        UserProfile profile = mock(UserProfile.class);
        UserStats stats = mock(UserStats.class);
        authenticate("ROLE_ADMIN");
        when(userAdminService.createUser(input)).thenReturn(profile);
        when(userAdminService.replaceUser(userId, profile)).thenReturn(profile);
        when(userAdminService.getStats()).thenReturn(stats);

        assertThat(controller.createUser(input).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.replaceUser(userId, profile).getBody()).isSameAs(profile);
        assertThat(controller.deleteUser(userId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.getUserStats().getBody()).isSameAs(stats);
        verify(userAdminService).deactivateUser(userId);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority(role))
        ));
    }
}
