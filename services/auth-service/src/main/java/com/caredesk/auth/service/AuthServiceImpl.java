package com.caredesk.auth.service;

import com.caredesk.auth.config.JwtUtil;
import com.caredesk.auth.exception.LoginFailedException;
import com.caredesk.auth.exception.ValidationException;
import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.openapitools.model.AuthSession;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String USER_DOES_NOT_EXIST = "User does not exist";
    private static final String WRONG_PASSWORD = "Wrong password";
    private static final String ACCOUNT_DEACTIVATED = "Account deactivated";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserProfileMapper userProfileMapper;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil,
                           UserProfileMapper userProfileMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public AuthSession register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        // Public self-registration always creates a PATIENT — elevated roles are assigned by admins only.
        user.setRole(Role.PATIENT);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getRole().name());
        return new AuthSession(token, userProfileMapper.toProfile(user));
    }

    @Override
    public AuthSession login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new LoginFailedException(USER_DOES_NOT_EXIST));
            String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getRole().name());
            return new AuthSession(token, userProfileMapper.toProfile(user));
        } catch (DisabledException ex) {
            throw new LoginFailedException(ACCOUNT_DEACTIVATED);
        } catch (UsernameNotFoundException ex) {
            throw new LoginFailedException(USER_DOES_NOT_EXIST);
        } catch (BadCredentialsException ex) {
            throw new LoginFailedException(WRONG_PASSWORD);
        }
    }

    @Override
    public void logout() {
        // JWT is stateless — logout is handled client-side by discarding the token.
        // A token blacklist can be added here later if needed.
    }
}
