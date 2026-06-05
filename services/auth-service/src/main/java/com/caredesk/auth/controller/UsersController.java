package com.caredesk.auth.controller;

import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.openapitools.api.UsersApi;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Controller for the {@code /users/**} endpoints.
 *
 * <p>Implements {@link UsersApi} and exposes the user directory owned by
 * auth-service. The patient-service deliberately stores only the clinical slice
 * of a profile (date of birth, phone), so name / email / role must be read from
 * here. The doctor dashboard uses {@code listUsers} to resolve patient names and
 * to search patients.
 *
 * <p>Requests are authenticated by the gateway-injected JWT (see
 * {@code JwtAuthFilter}). Per-role ownership rules are deferred to issue #32,
 * consistent with the other services. The {@code password} field is write-only
 * and is never populated on the way out.
 */
@Controller
public class UsersController implements UsersApi {

    private final UserRepository userRepository;

    /**
     * @param userRepository repository backing the user directory
     */
    public UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lists users, paged. Role and free-text filtering are applied client-side
     * by the dashboard, so no extra query parameters are needed here.
     *
     * @param page zero-based page index, defaulted to 0 by the API
     * @param size page size, defaulted to 20 by the API
     * @return 200 with the page of user profiles
     */
    @Override
    public ResponseEntity<PaginatedUserProfileResponse> listUsers(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> result = userRepository.findAll(pageable);
        List<UserProfile> content = result.getContent().stream()
                .map(this::toUserProfile)
                .toList();
        PageMeta meta = new PageMeta()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
        return ResponseEntity.ok(new PaginatedUserProfileResponse(content, meta));
    }

    /**
     * Returns a single user profile.
     *
     * @param userId the user's id
     * @return 200 with the profile, or 404 if no user exists with that id
     */
    @Override
    public ResponseEntity<UserProfile> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toUserProfile)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    // Maps a User entity to the generated UserProfile model. The internal Role
    // enum is converted to the API-layer UserRole at the boundary; the password
    // is intentionally left unset (writeOnly in the contract).
    private UserProfile toUserProfile(User user) {
        UserProfile profile = new UserProfile(
                user.getId(), user.getName(), user.getEmail(), UserRole.valueOf(user.getRole().name()));
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setDateOfBirth(user.getDateOfBirth());
        profile.setSpecialization(user.getSpecialization());
        profile.setLicenseNumber(user.getLicenseNumber());
        profile.setClinicId(user.getClinicId());
        return profile;
    }
}
