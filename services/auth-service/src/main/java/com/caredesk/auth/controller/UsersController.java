package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserService;
import org.openapitools.api.UsersApi;
import org.openapitools.model.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Controller for the {@code /users/**} endpoints.
 *
 * <p>Only {@code getUserById} is overridden so far. It is the path that
 * other services (patient-service, notes-service) hit to compose a full
 * {@link UserProfile} from auth-service's identity fields plus their own
 * domain data. Listing and replacing users will be added when the admin
 * dashboard work needs them.
 *
 * <p>This endpoint is intended for internal service-to-service calls. It is
 * permitted in {@code SecurityConfig} but only the gateway is exposed
 * outside the compose network, so external callers cannot reach it.
 */
@Controller
public class UsersController implements UsersApi {

    private final UserService userService;

    /**
     * @param userService the read-only user lookup service
     */
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the user profile for the given id.
     *
     * @param userId the user id
     * @return 200 with the profile, or 404 if no user exists
     */
    @Override
    public ResponseEntity<UserProfile> getUserById(UUID userId) {
        UserProfile profile = userService.findById(userId);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        return ResponseEntity.ok(profile);
    }
}
