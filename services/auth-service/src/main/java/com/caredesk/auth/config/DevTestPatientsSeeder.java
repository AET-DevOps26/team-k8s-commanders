package com.caredesk.auth.config;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;

/**
 * Optional dev-only seed data: 15 disposable test patients for UI testing.
 *
 * <p>Disable by setting {@code caredesk.seed.test-patients=false} or removing
 * {@code CAREDESK_SEED_TEST_PATIENTS} from compose. To remove seeded rows from
 * an existing database:
 *
 * <pre>{@code
 * DELETE FROM users WHERE email LIKE 'test-patient-%@caredesk.dev';
 * }</pre>
 *
 * <p>All accounts use password {@code patient123}.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "caredesk.seed", name = "test-patients", havingValue = "true")
public class DevTestPatientsSeeder implements ApplicationRunner {

    static final String EMAIL_DOMAIN = "@caredesk.dev";
    static final String EMAIL_PREFIX = "test-patient-";
    static final int TEST_PATIENT_COUNT = 15;

    private static final List<TestPatient> TEST_PATIENTS = buildTestPatients();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevTestPatientsSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        TEST_PATIENTS.forEach(this::upsert);
    }

    private void upsert(TestPatient testPatient) {
        User existing = userRepository.findById(testPatient.id())
                .or(() -> userRepository.findByEmail(testPatient.email()))
                .orElse(null);

        if (existing != null) {
            applyProfile(existing, testPatient);
            userRepository.save(existing);
            return;
        }

        User created = new User();
        created.setId(testPatient.id());
        created.setPassword(passwordEncoder.encode("patient123"));
        applyProfile(created, testPatient);
        userRepository.save(created);
    }

    private void applyProfile(User user, TestPatient testPatient) {
        user.setName(testPatient.name());
        user.setEmail(testPatient.email());
        user.setRole(Role.PATIENT);
        user.setPhoneNumber(testPatient.phoneNumber());
        user.setDateOfBirth(testPatient.dateOfBirth());
    }

    static UUID testPatientId(int index) {
        return UUID.fromString(String.format("bbbbbbb1-1111-1111-1111-%012d", index));
    }

    static String testPatientEmail(int index) {
        return EMAIL_PREFIX + String.format("%02d", index) + EMAIL_DOMAIN;
    }

    private static List<TestPatient> buildTestPatients() {
        return java.util.stream.IntStream.rangeClosed(1, TEST_PATIENT_COUNT)
                .mapToObj(index -> new TestPatient(
                        testPatientId(index),
                        "Test Patient " + String.format("%02d", index),
                        testPatientEmail(index),
                        "+49 89 200" + String.format("%03d", index),
                        LocalDate.parse("1985-01-01").plusDays(index - 1L)))
                .toList();
    }

    record TestPatient(UUID id, String name, String email, String phoneNumber, LocalDate dateOfBirth) {
    }
}
