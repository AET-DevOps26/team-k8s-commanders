package com.caredesk.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;

/**
 * Creates the initial production administrator from deployment-provided
 * configuration. Existing accounts and passwords are never modified.
 */
@Component
@ConditionalOnProperty(prefix = "caredesk.bootstrap.admin", name = "enabled", havingValue = "true")
public class ProductionAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionAdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public ProductionAdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${caredesk.bootstrap.admin.name:}") String name,
            @Value("${caredesk.bootstrap.admin.email:}") String email,
            @Value("${caredesk.bootstrap.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();

        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Configured production admin email belongs to a non-admin account");
            }
            log.info("Production admin account already exists; leaving credentials unchanged");
            return;
        }

        User admin = new User();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        log.info("Created production admin account");
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(name)
                || !StringUtils.hasText(email)
                || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "Production admin bootstrap is enabled but name, email, or password is missing");
        }
    }
}
