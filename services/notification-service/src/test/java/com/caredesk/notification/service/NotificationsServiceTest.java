package com.caredesk.notification.service;

import com.caredesk.notification.model.Notification;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationsService}. Backed by a mocked repository
 * so the tests do not need a database.
 */
class NotificationsServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationsService service = new NotificationsService(repository);

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
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
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
