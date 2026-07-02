package com.caredesk.notification.scheduler;

import com.caredesk.notification.client.PatientServiceClient;
import com.caredesk.notification.client.UpcomingAppointment;
import com.caredesk.notification.model.Notification;
import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.repository.NotificationRepository;
import com.caredesk.notification.service.NotificationsService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReminderScheduler}. The patient-service client,
 * notification service and repository are mocked so the scan logic is
 * exercised without HTTP, a database or SMTP.
 */
class ReminderSchedulerTest {

    private static final int MAX_ATTEMPTS = 5;

    private final PatientServiceClient patientClient = mock(PatientServiceClient.class);
    private final NotificationsService notificationsService = mock(NotificationsService.class);
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final ReminderScheduler scheduler =
            new ReminderScheduler(patientClient, notificationsService, repository, 24, MAX_ATTEMPTS);

    private static UpcomingAppointment appt(String email) {
        return new UpcomingAppointment(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusHours(5), email);
    }

    @Test
    void sendsReminderForUpcomingAppointmentNotYetReminded() {
        UpcomingAppointment appt = appt("anna@example.com");
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(appt));
        when(repository.findFirstByAppointmentIdAndType(appt.appointmentId(), NotificationType.REMINDER))
                .thenReturn(Optional.empty());

        scheduler.sendDueReminders();

        verify(notificationsService).recordAndSendReminder(
                eq(appt.appointmentId()), eq(appt.patientId()), eq("anna@example.com"), any(), any());
    }

    @Test
    void skipsAppointmentAlreadyDelivered() {
        UpcomingAppointment appt = appt("anna@example.com");
        Notification delivered = new Notification();
        delivered.setDelivered(true);
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(appt));
        when(repository.findFirstByAppointmentIdAndType(appt.appointmentId(), NotificationType.REMINDER))
                .thenReturn(Optional.of(delivered));

        scheduler.sendDueReminders();

        verify(notificationsService, never()).recordAndSendReminder(any(), any(), any(), any(), any());
    }

    @Test
    void skipsAppointmentWithNoDeliverableEmail() {
        UpcomingAppointment noEmail = appt("   ");
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(noEmail));

        scheduler.sendDueReminders();

        // No lookup and no record for a no-email appointment — avoids dead-row growth.
        verify(repository, never()).findFirstByAppointmentIdAndType(any(), any());
        verify(notificationsService, never()).recordAndSendReminder(any(), any(), any(), any(), any());
    }

    @Test
    void stopsRetryingOnceMaxAttemptsReached() {
        UpcomingAppointment appt = appt("anna@example.com");
        Notification exhausted = new Notification();
        exhausted.setDelivered(false);
        exhausted.setDeliveryAttempts(MAX_ATTEMPTS);
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(appt));
        when(repository.findFirstByAppointmentIdAndType(appt.appointmentId(), NotificationType.REMINDER))
                .thenReturn(Optional.of(exhausted));

        scheduler.sendDueReminders();

        verify(notificationsService, never()).recordAndSendReminder(any(), any(), any(), any(), any());
    }

    @Test
    void oneFailingAppointmentDoesNotStopTheRest() {
        UpcomingAppointment bad = appt("bad@example.com");
        UpcomingAppointment good = appt("good@example.com");
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(bad, good));
        when(repository.findFirstByAppointmentIdAndType(any(), eq(NotificationType.REMINDER)))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("boom"))
                .when(notificationsService)
                .recordAndSendReminder(eq(bad.appointmentId()), any(), any(), any(), any());

        scheduler.sendDueReminders();

        // The good appointment is still processed despite the bad one throwing.
        verify(notificationsService).recordAndSendReminder(eq(good.appointmentId()), any(), any(), any(), any());
    }

    @Test
    void doesNothingWhenNoUpcomingAppointments() {
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of());

        scheduler.sendDueReminders();

        verify(notificationsService, never()).recordAndSendReminder(any(), any(), any(), any(), any());
    }
}
