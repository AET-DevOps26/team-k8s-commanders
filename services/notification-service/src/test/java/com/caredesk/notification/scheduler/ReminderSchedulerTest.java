package com.caredesk.notification.scheduler;

import com.caredesk.notification.client.PatientServiceClient;
import com.caredesk.notification.client.UpcomingAppointment;
import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.repository.NotificationRepository;
import com.caredesk.notification.service.NotificationsService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private final PatientServiceClient patientClient = mock(PatientServiceClient.class);
    private final NotificationsService notificationsService = mock(NotificationsService.class);
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final ReminderScheduler scheduler =
            new ReminderScheduler(patientClient, notificationsService, repository, 24);

    @Test
    void sendsReminderForUpcomingAppointmentNotYetReminded() {
        UpcomingAppointment appt = new UpcomingAppointment(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusHours(5), "anna@example.com");
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(appt));
        when(repository.existsByAppointmentIdAndTypeAndDeliveredTrue(appt.appointmentId(), NotificationType.REMINDER))
                .thenReturn(false);

        scheduler.sendDueReminders();

        verify(notificationsService).recordAndSend(
                eq(appt.appointmentId()), eq(appt.patientId()), eq("anna@example.com"),
                eq(NotificationType.REMINDER), any(), any());
    }

    @Test
    void skipsAppointmentAlreadyReminded() {
        UpcomingAppointment appt = new UpcomingAppointment(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusHours(5), "anna@example.com");
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(appt));
        when(repository.existsByAppointmentIdAndTypeAndDeliveredTrue(appt.appointmentId(), NotificationType.REMINDER))
                .thenReturn(true);

        scheduler.sendDueReminders();

        verify(notificationsService, never()).recordAndSend(any(), any(), any(), any(), any(), any());
    }

    @Test
    void oneFailingAppointmentDoesNotStopTheRest() {
        UpcomingAppointment bad = new UpcomingAppointment(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusHours(2), "bad@example.com");
        UpcomingAppointment good = new UpcomingAppointment(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusHours(3), "good@example.com");
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of(bad, good));
        when(repository.existsByAppointmentIdAndTypeAndDeliveredTrue(any(), eq(NotificationType.REMINDER))).thenReturn(false);
        when(notificationsService.recordAndSend(eq(bad.appointmentId()), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        scheduler.sendDueReminders();

        // The good appointment is still processed despite the bad one throwing.
        verify(notificationsService).recordAndSend(eq(good.appointmentId()), any(), any(), any(), any(), any());
    }

    @Test
    void doesNothingWhenNoUpcomingAppointments() {
        when(patientClient.fetchUpcoming(24)).thenReturn(List.of());

        scheduler.sendDueReminders();

        verify(notificationsService, never()).recordAndSend(any(), any(), any(), any(), any(), any());
    }
}
