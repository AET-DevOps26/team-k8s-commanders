package com.caredesk.auth.service;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.UserCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only user management backing the {@code /users/**} endpoints. All callers
 * are authorised as ADMIN by Spring Security ({@code SecurityConfig}); this class
 * does not re-check the role.
 */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileMapper userProfileMapper;

    public UserAdminService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            UserProfileMapper userProfileMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userProfileMapper = userProfileMapper;
    }

    public PaginatedUserProfileResponse listUsers(int page, int size) {
        Page<User> result = userRepository.findAll(PageRequest.of(page, size));
        List<UserProfile> content = result.getContent().stream()
                .map(userProfileMapper::toProfile)
                .toList();
        PageMeta meta = new PageMeta(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
        return new PaginatedUserProfileResponse(content, meta);
    }

    public UserProfile getUser(UUID userId) {
        return userProfileMapper.toProfile(findOrThrow(userId));
    }

    public UserProfile createUser(UserCreate request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole().getValue()));
        user.setEnabled(true);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setSpecialization(request.getSpecialization());
        user.setLicenseNumber(request.getLicenseNumber());
        user.setClinicId(request.getClinicId());
        return userProfileMapper.toProfile(userRepository.save(user));
    }

    public UserProfile replaceUser(UUID userId, UserProfile request) {
        User user = findOrThrow(userId);

        // Reject email changes that collide with another account.
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(Role.valueOf(request.getRole().getValue()));
        }
        Boolean enabled = request.getEnabled();
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setSpecialization(request.getSpecialization());
        user.setLicenseNumber(request.getLicenseNumber());
        user.setClinicId(request.getClinicId());

        // Password is write-only and optional on update — only reset when provided.
        String password = request.getPassword();
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        return userProfileMapper.toProfile(userRepository.save(user));
    }

    public void deactivateUser(UUID userId) {
        User user = findOrThrow(userId);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public UserStats getStats() {
        long patients = userRepository.countByRole(Role.PATIENT);
        long doctors = userRepository.countByRole(Role.DOCTOR);
        long admins = userRepository.countByRole(Role.ADMIN);
        long active = userRepository.countByEnabled(true);
        long disabled = userRepository.countByEnabled(false);
        return new UserStats(patients + doctors + admins, patients, doctors, admins, active, disabled);
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
