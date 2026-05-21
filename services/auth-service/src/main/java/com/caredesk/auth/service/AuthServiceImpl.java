package com.caredesk.auth.service;

import com.caredesk.auth.config.JwtUtil;
import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.openapitools.model.AuthSession;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthSession register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Public self-registration always creates a PATIENT — elevated roles are assigned by admins only.
        user.setRole(Role.PATIENT);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthSession(token, toUserProfile(user));
    }

    @Override
    public AuthSession login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthSession(token, toUserProfile(user));
    }

    @Override
    public void logout() {
        // JWT is stateless — logout is handled client-side by discarding the token.
        // A token blacklist can be added here later if needed.
    }

    // Maps a User entity to the generated UserProfile model expected by the API contract.
    // Converts the internal Role enum to the API-layer UserRole at the boundary.
    private UserProfile toUserProfile(User user) {
        UserRole apiRole = UserRole.valueOf(user.getRole().name());
        UserProfile profile = new UserProfile(user.getId(), user.getName(), user.getEmail(), apiRole);
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setDateOfBirth(user.getDateOfBirth());
        profile.setSpecialization(user.getSpecialization());
        profile.setLicenseNumber(user.getLicenseNumber());
        profile.setClinicId(user.getClinicId());
        return profile;
    }
}
