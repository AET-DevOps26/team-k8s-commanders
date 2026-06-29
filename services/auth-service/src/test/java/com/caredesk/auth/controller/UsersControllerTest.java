package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserAccountService;
import com.caredesk.auth.service.UserAdminService;
import com.caredesk.auth.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority(role))
        ));
    }
}
