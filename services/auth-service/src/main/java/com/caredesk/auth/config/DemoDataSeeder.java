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
 * Seeds all demo user identities (patient, doctor and the second demo patient
 * "Anna Müller") with deterministic ids so other services can reference them.
 * This is the single seeder in auth-service and owns the demo <em>users</em>;
 * the other services' {@code DemoDataSeeder}s seed cross-service <em>data</em>
 * (appointments, notes, notifications) keyed on these ids.
 *
 * <p>Gated on the {@code dev} profile — the single switch for all demo seeding;
 * never runs in production. The administrator is deliberately <em>not</em>
 * seeded here: it is always created from deployment env vars by
 * {@link ProductionAdminSeeder}, so there is exactly one source of admin
 * accounts.
 */
@Component
@Profile("dev")
public class DemoDataSeeder implements ApplicationRunner {

    private static final List<DefaultUser> DEFAULT_USERS = List.of(
            new DefaultUser(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Patient", "patient@patient.com", "patient123", Role.PATIENT,
                    "+49 89 123456", LocalDate.parse("1990-04-12"), null, null,
                    null),
            new DefaultUser(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "Doctor", "doctor@doctor.com", "doctor123", Role.DOCTOR,
                    "+49 89 987654", LocalDate.parse("1982-09-21"), "General Medicine", "DE-CARE-1001",
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            // Extra doctors so the booking flow has several specializations to
            // choose from. Their ids match the DOCTOR_* constants in
            // patient-service's demo seeder, which gives each a few open slots.
            new DefaultUser(UUID.fromString("22222222-2222-2222-2222-000000000002"),
                    "Dr. Sarah Chen", "sarah.chen@caredesk.dev", "doctor123", Role.DOCTOR,
                    "+49 89 222001", LocalDate.parse("1985-03-15"), "Cardiology", "DE-CARE-1002",
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            new DefaultUser(UUID.fromString("22222222-2222-2222-2222-000000000003"),
                    "Dr. Tom Becker", "tom.becker@caredesk.dev", "doctor123", Role.DOCTOR,
                    "+49 89 222002", LocalDate.parse("1979-11-02"), "Pediatrics", "DE-CARE-1003",
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            // A second General Medicine doctor so the doctor dropdown has more
            // than one option once a specialization is chosen.
            new DefaultUser(UUID.fromString("22222222-2222-2222-2222-000000000004"),
                    "Dr. Mark Lopez", "mark.lopez@caredesk.dev", "doctor123", Role.DOCTOR,
                    "+49 89 222003", LocalDate.parse("1988-07-08"), "General Medicine", "DE-CARE-1004",
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            // Second demo patient — "Anna Müller", the pitch's AI-assistant example.
            // This fixed id must match ANNA_ID in patient-service's and
            // notification-service's demo seeders so her appointments, notes and
            // notifications line up across services.
            new DefaultUser(UUID.fromString("d0000000-0000-0000-0000-0000000000a1"),
                    "Anna Müller", "anna.mueller@caredesk.dev", "patient123", Role.PATIENT,
                    "+49 89 445566", LocalDate.parse("1975-06-30"), null, null,
                    null),
            // Additional demo patients so the patient roster has more than one
            // or two entries to browse. Their ids match MAX_ID / LENA_ID in
            // patient-service's and notes-service's demo seeders, which give
            // each of them a spread of appointments (completed, rescheduled,
            // cancelled) plus clinical notes on the completed visits.
            new DefaultUser(UUID.fromString("d0000000-0000-0000-0000-0000000000a2"),
                    "Max Schmidt", "max.schmidt@caredesk.dev", "patient123", Role.PATIENT,
                    "+49 89 445577", LocalDate.parse("1993-02-17"), null, null,
                    null),
            new DefaultUser(UUID.fromString("d0000000-0000-0000-0000-0000000000a3"),
                    "Lena Fischer", "lena.fischer@caredesk.dev", "patient123", Role.PATIENT,
                    "+49 89 445588", LocalDate.parse("1968-11-05"), null, null,
                    null)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
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
