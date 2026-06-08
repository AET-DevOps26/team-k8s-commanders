package com.caredesk.auth.service;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.PasswordChangeRequest;
import org.openapitools.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfile getUser(UUID userId) {
        User user = findUser(userId);
        requireOwnerOrAdmin(user);
        return UserService.toUserProfile(user);
    }

    @Transactional(readOnly = true)
    public PaginatedUserProfileResponse listUsers(int page, int size) {
        requireAdmin();
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        List<UserProfile> content = users.getContent().stream()
                .map(UserService::toUserProfile)
                .toList();
        PageMeta pageMeta = new PageMeta(page, size, users.getTotalElements(), users.getTotalPages());
        return new PaginatedUserProfileResponse(content, pageMeta);
    }

    @Transactional
    public UserProfile updateUser(UUID userId, UserProfile request) {
        User user = findUser(userId);
        requireOwnerOrAdmin(user);
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new DuplicateEmailException("Email already registered");
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (isAdmin()) {
            applyAdminFields(user, request);
        }
        return UserService.toUserProfile(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = findUser(userId);
        requireOwnerOrAdmin(user);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is invalid");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void requireOwnerOrAdmin(User target) {
        if (isAdmin()) {
            return;
        }
        User current = currentUser();
        if (!current.getId().equals(target.getId())) {
            throw new AccessDeniedException("Cannot access another user's account");
        }
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BadCredentialsException("Authentication required");
        }
        Object principal = authentication.getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String username) {
            email = username;
        } else {
            throw new BadCredentialsException("Authentication required");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
    }

    private void applyAdminFields(User user, UserProfile request) {
        if (request.getRole() != null) {
            user.setRole(Role.valueOf(request.getRole().name()));
        }
        user.setSpecialization(request.getSpecialization());
        user.setLicenseNumber(request.getLicenseNumber());
        user.setClinicId(request.getClinicId());
    }

}
