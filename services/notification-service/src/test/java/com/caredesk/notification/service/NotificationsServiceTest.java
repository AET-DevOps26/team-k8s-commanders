package com.caredesk.notification.service;

import com.caredesk.notification.email.EmailSender;
import com.caredesk.notification.model.Notification;
import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.model.NotificationChannel;
import org.openapitools.model.NotificationCreate;
import org.openapitools.model.PaginatedNotificationResponse;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationsService}. Backed by a mocked repository
 * so the tests do not need a database.
 */
class NotificationsServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final NotificationsService service = new NotificationsService(repository, emailSender);

    @Test
    void create_persistsRecordAndStampsSentAt() {
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        NotificationCreate request = new NotificationCreate("Your appointment is confirmed", NotificationChannel.EMAIL);
        request.setPatientId(patientId);
        request.setAppointmentId(appointmentId);
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        org.openapitools.model.Notification created = service.create(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getPatientId()).isEqualTo(patientId);
        assertThat(persisted.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(persisted.getMessage()).isEqualTo("Your appointment is confirmed");
        assertThat(persisted.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(persisted.getSentAt()).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSentAt()).isEqualTo(persisted.getSentAt());
    }

    @Test
    void list_returnsAllNotifications_forAdmin() {
        Notification n = notification(UUID.randomUUID(), UUID.randomUUID());
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        PaginatedNotificationResponse response = service.list(UserRole.ADMIN, UUID.randomUUID(), 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage().getTotalElements()).isEqualTo(1);
        verify(repository, never()).findByPatientId(any(), any());
    }

    @Test
    void list_scopesToOwnNotifications_forPatient() {
        UUID patientId = UUID.randomUUID();
        Notification n = notification(patientId, UUID.randomUUID());
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(repository.findByPatientId(eq(patientId), any(Pageable.class))).thenReturn(page);

        PaginatedNotificationResponse response = service.list(UserRole.PATIENT, patientId, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPatientId()).isEqualTo(patientId);
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getById_returnsNotification_forRecipientPatient() {
        UUID patientId = UUID.randomUUID();
        Notification n = notification(patientId, UUID.randomUUID());
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));

        org.openapitools.model.Notification dto = service.getById(n.getId(), UserRole.PATIENT, patientId);

        assertThat(dto.getId()).isEqualTo(n.getId());
        assertThat(dto.getMessage()).isEqualTo(n.getMessage());
    }

    @Test
    void getById_returnsAnyNotification_forAdmin() {
        Notification n = notification(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));

        org.openapitools.model.Notification dto = service.getById(n.getId(), UserRole.ADMIN, UUID.randomUUID());

        assertThat(dto.getId()).isEqualTo(n.getId());
    }

    @Test
    void getById_deniesOtherPatientsNotification() {
        Notification n = notification(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.getById(n.getId(), UserRole.PATIENT, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id, UserRole.ADMIN, UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listForAppointment_returnsAllAppointmentNotifications_forAdmin() {
        UUID appointmentId = UUID.randomUUID();
        Notification n = notification(UUID.randomUUID(), appointmentId);
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(repository.findByAppointmentId(eq(appointmentId), any(Pageable.class))).thenReturn(page);

        PaginatedNotificationResponse response =
                service.listForAppointment(appointmentId, UserRole.ADMIN, UUID.randomUUID(), 0, 20);

        assertThat(response.getContent()).hasSize(1);
        verify(repository, never()).findByAppointmentIdAndPatientId(any(), any(), any());
    }

    @Test
    void listForAppointment_scopesToOwnNotifications_forPatient() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Notification n = notification(patientId, appointmentId);
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(repository.findByAppointmentIdAndPatientId(eq(appointmentId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        PaginatedNotificationResponse response =
                service.listForAppointment(appointmentId, UserRole.PATIENT, patientId, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        verify(repository, never()).findByAppointmentId(any(), any());
    }

    @Test
    void recordAndSend_persistsRecordThenSendsEmail() {
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId(UUID.randomUUID());
            }
            return n;
        });
        when(emailSender.send(any(), any(), any())).thenReturn(true);

        org.openapitools.model.Notification created = service.recordAndSend(
                appointmentId, patientId, "anna@example.com",
                NotificationType.CONFIRMATION, "Appointment confirmed", "You're booked.");

        // Saved twice: once before send, then again to record the delivery outcome.
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getType()).isEqualTo(NotificationType.CONFIRMATION);
        assertThat(persisted.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(persisted.getRecipientEmail()).isEqualTo("anna@example.com");
        assertThat(persisted.getMessage()).isEqualTo("You're booked.");
        assertThat(persisted.getSentAt()).isNotNull();
        // Delivery succeeded, so the record is marked delivered.
        assertThat(persisted.isDelivered()).isTrue();
        verify(emailSender).send("anna@example.com", "Appointment confirmed", "You're booked.");
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void recordAndSend_persistsRecordEvenWhenDeliveryFails() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        // EmailSender swallows failures and returns false; recordAndSend must still return the saved record.
        when(emailSender.send(any(), any(), any())).thenReturn(false);

        org.openapitools.model.Notification created = service.recordAndSend(
                UUID.randomUUID(), UUID.randomUUID(), "down@example.com",
                NotificationType.REMINDER, "Reminder", "See you soon.");

        // Still persisted (twice), but marked undelivered so the scheduler retries it.
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getValue().isDelivered()).isFalse();
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void recordAndSendReminder_createsSingleRowAndCountsAttempt() {
        UUID appointmentId = UUID.randomUUID();
        when(repository.findFirstByAppointmentIdAndType(appointmentId, NotificationType.REMINDER))
                .thenReturn(Optional.empty());
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailSender.send(any(), any(), any())).thenReturn(true);

        service.recordAndSendReminder(appointmentId, UUID.randomUUID(), "anna@example.com",
                "Appointment reminder", "See you soon.");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.REMINDER);
        assertThat(saved.getDeliveryAttempts()).isEqualTo(1);
        assertThat(saved.isDelivered()).isTrue();
    }

    @Test
    void recordAndSendReminder_reusesExistingRowAndIncrementsAttempts() {
        UUID appointmentId = UUID.randomUUID();
        Notification existing = new Notification();
        existing.setId(UUID.randomUUID());
        existing.setAppointmentId(appointmentId);
        existing.setType(NotificationType.REMINDER);
        existing.setDeliveryAttempts(2);
        when(repository.findFirstByAppointmentIdAndType(appointmentId, NotificationType.REMINDER))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailSender.send(any(), any(), any())).thenReturn(false);

        service.recordAndSendReminder(appointmentId, UUID.randomUUID(), "anna@example.com",
                "Appointment reminder", "See you soon.");

        // The same row is updated (not a new insert), attempts bumped, still undelivered.
        assertThat(existing.getDeliveryAttempts()).isEqualTo(3);
        assertThat(existing.isDelivered()).isFalse();
    }

    private static Notification notification(UUID patientId, UUID appointmentId) {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setPatientId(patientId);
        n.setAppointmentId(appointmentId);
        n.setMessage("Reminder: upcoming appointment");
        n.setChannel(NotificationChannel.EMAIL);
        n.setSentAt(OffsetDateTime.now().minusMinutes(5));
        return n;
    }
}
