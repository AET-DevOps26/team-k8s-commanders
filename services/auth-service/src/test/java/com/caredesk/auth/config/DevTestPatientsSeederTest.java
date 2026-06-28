package com.caredesk.auth.config;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevTestPatientsSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DevTestPatientsSeeder seeder = new DevTestPatientsSeeder(userRepository, passwordEncoder);

    @Test
    void seedsFifteenTestPatients() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));

        seeder.run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(DevTestPatientsSeeder.TEST_PATIENT_COUNT)).save(userCaptor.capture());

        assertThat(userCaptor.getAllValues())
                .hasSize(DevTestPatientsSeeder.TEST_PATIENT_COUNT)
                .allMatch(user -> user.getRole() == Role.PATIENT)
                .allMatch(user -> user.getEmail().startsWith(DevTestPatientsSeeder.EMAIL_PREFIX))
                .allMatch(user -> user.getEmail().endsWith(DevTestPatientsSeeder.EMAIL_DOMAIN));
    }
}
