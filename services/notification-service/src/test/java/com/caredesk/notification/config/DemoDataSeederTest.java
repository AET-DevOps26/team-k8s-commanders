package com.caredesk.notification.config;

import com.caredesk.notification.model.Notification;
import com.caredesk.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.NotificationChannel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final DemoDataSeeder seeder = new DemoDataSeeder(notificationRepository);

    @Test
    void seedsConfirmationAndReminderRecords() {
        when(notificationRepository.findById(any())).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<Notification> notifications = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(4)).save(notifications.capture());

        List<Notification> saved = notifications.getAllValues();
        assertThat(saved).allSatisfy(n -> {
            assertThat(n.getId()).isNotNull();
            assertThat(n.getAppointmentId()).isNotNull();
            assertThat(n.getPatientId()).isNotNull();
            assertThat(n.getMessage()).isNotBlank();
            assertThat(n.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(n.getSentAt()).isNotNull();
        });
        // At least one reminder-style message is present.
        assertThat(saved).anySatisfy(n -> assertThat(n.getMessage()).containsIgnoringCase("reminder"));
    }

    @Test
    void isIdempotent_reusingExistingRecords() {
        when(notificationRepository.findById(any())).thenReturn(Optional.of(new Notification()));

        seeder.run(null);

        verify(notificationRepository, times(4)).save(any(Notification.class));
    }
}
