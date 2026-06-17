package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserAccountService;
import com.caredesk.auth.service.UserAdminService;
import com.caredesk.auth.service.UserService;
import org.openapitools.api.UsersApi;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.PasswordChangeRequest;
import org.openapitools.model.UserCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserStats;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Implements the generated {@link UsersApi} for the {@code /users/**} endpoints.
 *
 * <p>Admin operations ({@code listUsers}, {@code createUser}, {@code deleteUser},
 * {@code getUserStats}) delegate to {@link UserAdminService}. Gateway-facing
 * account self-service ({@code changeUserPassword}, patient profile updates via
 * {@code replaceUser}) delegate to {@link UserAccountService}, which enforces
 * owner-or-admin authorization.
 *
 * <p>{@code getUserById} serves two callers: authenticated users reach it through
 * the gateway with a JWT, while patient-service and notes-service call it directly
 * on the compose network without credentials. Unauthenticated internal calls use
 * the read-only {@link UserService} path; authenticated gateway calls use
 * {@link UserAccountService}.
 *
 * <p>The doctor dashboard uses {@code listUsers} to resolve patient names and
 * search patients. The patient-service deliberately stores only the clinical
 * slice of a profile (date of birth, phone), so name / email / role must be
 * read from here.
 */
@Controller
public class UsersController implements UsersApi {

    private final UserAccountService userAccountService;
    private final UserService userService;
    private final UserAdminService userAdminService;

    public UsersController(UserAccountService userAccountService,
                           UserService userService,
                           UserAdminService userAdminService) {
        this.userAccountService = userAccountService;
        this.userService = userService;
        this.userAdminService = userAdminService;
    }

    @Override
    public ResponseEntity<PaginatedUserProfileResponse> listUsers(Integer page, Integer size) {
        if (isAdmin()) {
            return ResponseEntity.ok(userAdminService.listUsers(page, size));
        }
        return ResponseEntity.ok(userAccountService.listUsers(page, size));
    }

    @Override
    public ResponseEntity<Void> changeUserPassword(UUID userId, PasswordChangeRequest passwordChangeRequest) {
        userAccountService.changePassword(userId, passwordChangeRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserProfile> getUserById(UUID userId) {
        if (isAuthenticated()) {
            return ResponseEntity.ok(userAccountService.getUser(userId));
        }
        UserProfile profile = userService.findById(userId);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        return ResponseEntity.ok(profile);
    }

    @Override
    public ResponseEntity<UserProfile> createUser(UserCreate userCreate) {
        UserProfile created = userAdminService.createUser(userCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<UserProfile> replaceUser(UUID userId, UserProfile userProfile) {
        if (isAdmin()) {
            return ResponseEntity.ok(userAdminService.replaceUser(userId, userProfile));
        }
        return ResponseEntity.ok(userAccountService.updateUser(userId, userProfile));
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

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
