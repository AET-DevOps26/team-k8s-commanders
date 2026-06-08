package com.caredesk.auth.config;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;

/**
 * Seeds deterministic dev users so other services can reference stable ids.
 */
@Component
@Profile("dev")
public class DefaultUserSeeder implements ApplicationRunner {

    private static final List<DefaultUser> DEFAULT_USERS = List.of(
            new DefaultUser(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Patient", "patient@patient.com", "patient123", Role.PATIENT,
                    "+49 89 123456", LocalDate.parse("1990-04-12"), null, null,
                    null),
            new DefaultUser(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "Doctor", "doctor@doctor.com", "doctor123", Role.DOCTOR,
                    "+49 89 987654", LocalDate.parse("1982-09-21"), "General Medicine", "DE-CARE-1001",
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            new DefaultUser(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    "Admin", "admin@admin.com", "admin123", Role.ADMIN,
                    "+49 89 555000", LocalDate.parse("1978-02-03"), null, null,
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULT_USERS.forEach(this::upsert);
    }

    private void upsert(DefaultUser defaultUser) {
        User existing = userRepository.findById(defaultUser.id())
                .or(() -> userRepository.findByEmail(defaultUser.email()))
                .orElse(null);

        if (existing != null) {
            applyProfile(existing, defaultUser);
            userRepository.save(existing);
            return;
        }

        User created = new User();
        created.setId(defaultUser.id());
        created.setPassword(passwordEncoder.encode(defaultUser.password()));
        applyProfile(created, defaultUser);
        userRepository.save(created);
    }

    private void applyProfile(User user, DefaultUser defaultUser) {
        user.setName(defaultUser.name());
        user.setEmail(defaultUser.email());
        user.setRole(defaultUser.role());
        user.setPhoneNumber(defaultUser.phoneNumber());
        user.setDateOfBirth(defaultUser.dateOfBirth());
        user.setSpecialization(defaultUser.specialization());
        user.setLicenseNumber(defaultUser.licenseNumber());
        user.setClinicId(defaultUser.clinicId());
    }

    private record DefaultUser(UUID id,
                               String name,
                               String email,
                               String password,
                               Role role,
                               String phoneNumber,
                               LocalDate dateOfBirth,
                               String specialization,
                               String licenseNumber,
                               UUID clinicId) {
    }
}
