package com.caredesk.auth.config;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DemoDataSeeder seeder = new DemoDataSeeder(userRepository, passwordEncoder);

    @Test
    void createsAnnaAsPatientWithEncodedPassword() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        seeder.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User anna = captor.getValue();
        assertThat(anna.getId()).isEqualTo(DemoDataSeeder.ANNA_ID);
        assertThat(anna.getName()).isEqualTo("Anna Müller");
        assertThat(anna.getRole()).isEqualTo(Role.PATIENT);
        assertThat(anna.getPassword()).isEqualTo("hashed");
    }

    @Test
    void leavesExistingPasswordUnchanged() {
        User existing = new User();
        existing.setId(DemoDataSeeder.ANNA_ID);
        existing.setPassword("already-hashed");
        when(userRepository.findById(DemoDataSeeder.ANNA_ID)).thenReturn(Optional.of(existing));

        seeder.run(null);

        verify(passwordEncoder, never()).encode(any());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("already-hashed");
    }
}
