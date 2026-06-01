package com.caredesk.auth.config;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
public class DefaultUserSeeder implements ApplicationRunner {

    private static final List<DefaultUser> DEFAULT_USERS = List.of(
            new DefaultUser("Patient", "patient@patient.com", "patient123", Role.PATIENT),
            new DefaultUser("Doctor", "doctor@doctor.com", "doctor123", Role.DOCTOR),
            new DefaultUser("Admin", "admin@admin.com", "admin123", Role.ADMIN)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        DEFAULT_USERS.stream()
                .filter(defaultUser -> !userRepository.existsByEmail(defaultUser.email()))
                .map(this::toEntity)
                .forEach(userRepository::save);
    }

    private User toEntity(DefaultUser defaultUser) {
        User user = new User();
        user.setName(defaultUser.name());
        user.setEmail(defaultUser.email());
        user.setPassword(passwordEncoder.encode(defaultUser.password()));
        user.setRole(defaultUser.role());
        return user;
    }

    private record DefaultUser(String name, String email, String password, Role role) {
    }
}
