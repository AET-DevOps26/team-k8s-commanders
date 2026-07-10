package com.caredesk.auth.controller;

import com.caredesk.auth.service.AuthService;
import org.openapitools.api.AuthApi;
import org.openapitools.model.AuthSession;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

// Implements AuthApi — the interface generated from the OpenAPI spec.
// All endpoint paths, HTTP methods, and response types come from AuthApi.
// No /api/v1 prefix here — the gateway owns that. Endpoints are /auth/register, /auth/login, /auth/logout.
@Controller
public class AuthController implements AuthApi {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<AuthSession> registerUser(RegisterRequest registerRequest) {
        AuthSession session = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @Override
    public ResponseEntity<AuthSession> loginUser(LoginRequest loginRequest) {
        AuthSession session = authService.login(loginRequest);
        return ResponseEntity.ok(session);
    }

    @Override
    @PostMapping(AuthApi.PATH_LOGOUT_USER)
    public ResponseEntity<Void> logoutUser() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
