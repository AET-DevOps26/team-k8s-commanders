package com.caredesk.auth.repository;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void seed() {
        userRepository.save(doctor("Alice Smith", "alice@x.com", "Cardiology", true));
        userRepository.save(doctor("Bob Jones", "bob@x.com", "Dermatology", true));
        userRepository.save(doctor("Carol Disabled", "carol@x.com", "Cardiology", false));
        userRepository.save(patient("Pat Ient", "pat@x.com"));
    }

    @Test
    void searchDoctors_returnsOnlyEnabledDoctors_orderedByName() {
        Page<User> result = userRepository.searchDoctors("", "", PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(User::getName)
                .containsExactly("Alice Smith", "Bob Jones");
    }

    @Test
    void searchDoctors_matchesNameOrSpecialization() {
        assertThat(userRepository.searchDoctors("cardio", "", PageRequest.of(0, 20)).getContent())
                .extracting(User::getName).containsExactly("Alice Smith");

        assertThat(userRepository.searchDoctors("jones", "", PageRequest.of(0, 20)).getContent())
                .extracting(User::getName).containsExactly("Bob Jones");
    }

    @Test
    void searchDoctors_filtersBySpecialization() {
        assertThat(userRepository.searchDoctors("", "dermatology", PageRequest.of(0, 20)).getContent())
                .extracting(User::getName).containsExactly("Bob Jones");
    }

    @Test
    void findDistinctSpecializations_returnsSortedDistinctOfEnabledDoctorsOnly() {
        // Cardiology (Alice, enabled) + Dermatology (Bob, enabled); Carol's
        // Cardiology is disabled so it must not add a duplicate, and the patient
        // has no specialization.
        assertThat(userRepository.findDistinctSpecializations())
                .containsExactly("Cardiology", "Dermatology");
    }

    private static User doctor(String name, String email, String specialization, boolean enabled) {
        User user = base(name, email, Role.DOCTOR);
        user.setSpecialization(specialization);
        user.setEnabled(enabled);
        return user;
    }

    private static User patient(String name, String email) {
        return base(name, email, Role.PATIENT);
    }

    private static User base(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
