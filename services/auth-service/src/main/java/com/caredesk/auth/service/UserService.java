package com.caredesk.auth.service;

import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read-only lookups for {@link User} rows, returned as the API-shaped
 * {@link UserProfile}.
 *
 * <p>Used both by the public {@code /auth/**} flows and by the internal
 * {@code /users/{id}} endpoint that other services call to compose a full
 * profile view from their own domain data plus the identity fields owned by
 * auth-service.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * @param userRepository read access to the users table
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by id and maps it to a {@link UserProfile}.
     *
     * @param id the user id
     * @return the profile, or {@code null} if no user exists with that id
     */
    public UserProfile findById(UUID id) {
        return userRepository.findById(id).map(UserService::toUserProfile).orElse(null);
    }

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Searches enabled doctors by name / specialization for the internal
     * doctor-directory endpoint that patient-service composes into its
     * {@code /doctors} listing.
     *
     * <p>Directory entries are mapped with {@link #toDirectoryProfile} so a
     * doctor's personal contact details are not exposed to booking patients.
     */
    public PaginatedUserProfileResponse searchDoctors(String q, String specialization, int page, int size) {
        Page<User> result = userRepository.searchDoctors(
                blankToEmpty(q), blankToEmpty(specialization),
                PageRequest.of(Math.max(page, 0), clampSize(size)));
        List<UserProfile> content = result.getContent().stream()
                .map(UserService::toDirectoryProfile)
                .toList();
        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
        return new PaginatedUserProfileResponse(content, meta);
    }

    /**
     * Distinct specializations across enabled doctors, for the booking flow's
     * specialization filter. Sourced from auth-service because it owns doctor
     * identity, so a specialization exists exactly when a doctor has it.
     */
    public List<String> listSpecializations() {
        return userRepository.findDistinctSpecializations();
    }

    private static int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    /**
     * Maps a doctor to the public directory view shown to patients: identity and
     * professional fields only, omitting personal contact details
     * ({@code phoneNumber}, {@code dateOfBirth}) that a booking patient has no
     * need to see.
     */
    private static UserProfile toDirectoryProfile(User user) {
        UserProfile profile = new UserProfile(
                user.getId(),
                user.getName(),
                user.getEmail(),
                UserRole.valueOf(user.getRole().name())
        );
        profile.setSpecialization(user.getSpecialization());
        profile.setLicenseNumber(user.getLicenseNumber());
        profile.setClinicId(user.getClinicId());
        return profile;
    }

    /**
     * Maps a {@link User} entity to a {@link UserProfile} DTO, copying the
     * core identity fields plus any optional profile fields the entity
     * carries.
     *
     * @param user the JPA entity
     * @return a new {@code UserProfile} with the same values
     */
    public static UserProfile toUserProfile(User user) {
        UserProfile profile = new UserProfile(
                user.getId(),
                user.getName(),
                user.getEmail(),
                UserRole.valueOf(user.getRole().name())
        );
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setDateOfBirth(user.getDateOfBirth());
        profile.setSpecialization(user.getSpecialization());
        profile.setLicenseNumber(user.getLicenseNumber());
        profile.setClinicId(user.getClinicId());
        return profile;
    }
}
