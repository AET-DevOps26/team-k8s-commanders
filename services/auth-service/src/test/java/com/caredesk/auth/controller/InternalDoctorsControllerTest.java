package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PaginatedUserProfileResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalDoctorsControllerTest {

    private final UserService userService = mock(UserService.class);
    private final InternalDoctorsController controller = new InternalDoctorsController(userService);

    @Test
    void searchDelegatesAllDirectoryFilters() {
        PaginatedUserProfileResponse page = mock(PaginatedUserProfileResponse.class);
        when(userService.searchDoctors("lee", "Cardiology", 2, 10)).thenReturn(page);

        assertThat(controller.searchDoctors("lee", "Cardiology", 2, 10)).isSameAs(page);
    }

    @Test
    void specializationsComeFromAuthSourceOfTruth() {
        when(userService.listSpecializations()).thenReturn(List.of("Cardiology", "General Medicine"));

        assertThat(controller.listSpecializations())
                .containsExactly("Cardiology", "General Medicine");
        verify(userService).listSpecializations();
    }
}
