package com.caredesk.auth.service;

import org.openapitools.model.AuthSession;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;

public interface AuthService {
    AuthSession register(RegisterRequest request);
    AuthSession login(LoginRequest request);
    void logout();
}
