package com.caredesk.auth.config;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Seeds demo user identities that the cross-service demo dataset (issue #168)
 * depends on. The canonical patient / doctor / admin are already created by
 * {@link DefaultUserSeeder}; this adds the second demo patient, "Anna Müller",
 * whose visit history is the pitch's AI-assistant example.
 *
 * <p>Enabled only in the {@code dev} profile and when
 * {@code caredesk.seed.demo=true} (env {@code CAREDESK_SEED_DEMO}); never runs
 * in production. Idempotent via an id/email upsert.
 *
 * <p>The {@code ANNA_ID} here must match the same constant in patient-service's
 * demo seeder so her profile, appointments and notes line up.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "caredesk.seed", name = "demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    static final UUID ANNA_ID = UUID.fromString("d0000000-0000-0000-0000-0000000000a1");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository  user rows
     * @param passwordEncoder BCrypt encoder for the demo password
     */
    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Upserts the "Anna Müller" demo patient account.
     *
     * @param args ignored
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User anna = userRepository.findById(ANNA_ID)
                .or(() -> userRepository.findByEmail("anna.mueller@caredesk.dev"))
                .orElseGet(User::new);
        anna.setId(ANNA_ID);
        anna.setName("Anna Müller");
        anna.setEmail("anna.mueller@caredesk.dev");
        // Only set the password on first creation, so an operator-changed password is never reset.
        if (anna.getPassword() == null || anna.getPassword().isBlank()) {
            anna.setPassword(passwordEncoder.encode("patient123"));
        }
        anna.setRole(Role.PATIENT);
        anna.setEnabled(true);
        anna.setPhoneNumber("+49 89 445566");
        anna.setDateOfBirth(LocalDate.parse("1975-06-30"));
        userRepository.save(anna);

        log.info("Demo dataset seeded (auth-service): demo patient Anna Müller");
    }
}
