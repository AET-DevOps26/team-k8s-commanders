package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserAdminService;
import org.openapitools.api.UsersApi;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.UserCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserStats;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Implements the generated {@link UsersApi} for the {@code /users/**} endpoints.
 * These are admin-only — access is enforced by Spring Security in
 * {@code SecurityConfig} (role taken from the JWT). Business logic is delegated
 * to {@link UserAdminService}.
 */
@Controller
public class UsersController implements UsersApi {

    private final UserAdminService userAdminService;

    public UsersController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @Override
    public ResponseEntity<PaginatedUserProfileResponse> listUsers(Integer page, Integer size) {
        return ResponseEntity.ok(userAdminService.listUsers(page, size));
    }

    @Override
    public ResponseEntity<UserProfile> getUserById(UUID userId) {
        return ResponseEntity.ok(userAdminService.getUser(userId));
    }

    @Override
    public ResponseEntity<UserProfile> createUser(UserCreate userCreate) {
        UserProfile created = userAdminService.createUser(userCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<UserProfile> replaceUser(UUID userId, UserProfile userProfile) {
        return ResponseEntity.ok(userAdminService.replaceUser(userId, userProfile));
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID userId) {
        userAdminService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserStats> getUserStats() {
        return ResponseEntity.ok(userAdminService.getStats());
    }
}
