package com.caredesk.auth.controller;

import com.caredesk.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService))
            .build();

    @Test
    void logoutAcceptsJsonClients() throws Exception {
        mockMvc.perform(post("/auth/logout").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(authService).logout();
    }
}
