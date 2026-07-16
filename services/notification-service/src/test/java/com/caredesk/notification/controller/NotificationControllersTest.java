package com.caredesk.notification.controller;

import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.service.NotificationsService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.openapitools.model.Notification;
import org.openapitools.model.NotificationChannel;
import org.openapitools.model.NotificationCreate;
import org.openapitools.model.PaginatedNotificationResponse;
import org.openapitools.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationControllersTest {

    private final NotificationsService service = mock(NotificationsService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void createRejectsBlankMessageBeforePersistence() {
        NotificationsController controller = new NotificationsController(service, request);
        NotificationCreate input = new NotificationCreate("  ", NotificationChannel.EMAIL);

        assertThatThrownBy(() -> controller.createNotification(input))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(service);
    }

    @Test
    void listUsesTrustedIdentityAndOpenApiPaginationDefaults() {
        UUID userId = patientIdentity();
        PaginatedNotificationResponse page = mock(PaginatedNotificationResponse.class);
        when(service.list(UserRole.PATIENT, userId, 0, 20)).thenReturn(page);
        NotificationsController controller = new NotificationsController(service, request);

        assertThat(controller.listNotifications(null, null).getBody()).isSameAs(page);
    }

    @Test
    void appointmentListScopesQueryToCaller() {
        UUID userId = patientIdentity();
        UUID appointmentId = UUID.randomUUID();
        PaginatedNotificationResponse page = mock(PaginatedNotificationResponse.class);
        when(service.listForAppointment(appointmentId, UserRole.PATIENT, userId, 1, 5)).thenReturn(page);
        AppointmentNotificationsController controller =
                new AppointmentNotificationsController(service, request);

        assertThat(controller.listAppointmentNotifications(appointmentId, 1, 5).getBody()).isSameAs(page);
    }

    @Test
    void malformedRoleIsRejectedBeforeRead() {
        when(request.getHeader("X-User-Role")).thenReturn("OWNER");
        NotificationsController controller = new NotificationsController(service, request);

        assertThatThrownBy(() -> controller.getNotificationById(UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(service);
    }

    @Test
    void internalTriggerAppliesSafeDefaultsAndReturnsCreated() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        InternalNotificationRequest input = new InternalNotificationRequest(
                appointmentId, patientId, "patient@example.com", null, null, "Booked");
        Notification created = mock(Notification.class);
        when(service.recordAndSend(appointmentId, patientId, "patient@example.com",
                NotificationType.GENERIC, "CareDesk notification", "Booked")).thenReturn(created);
        InternalNotificationsController controller = new InternalNotificationsController(service);

        var response = controller.create(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    void internalTriggerRejectsMissingMessage() {
        InternalNotificationsController controller = new InternalNotificationsController(service);
        InternalNotificationRequest input = new InternalNotificationRequest(
                UUID.randomUUID(), UUID.randomUUID(), "patient@example.com",
                NotificationType.CONFIRMATION, "Subject", null);

        assertThatThrownBy(() -> controller.create(input))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(service);
    }

    private UUID patientIdentity() {
        UUID userId = UUID.randomUUID();
        when(request.getHeader("X-User-Role")).thenReturn("patient");
        when(request.getHeader("X-User-Id")).thenReturn(userId.toString());
        return userId;
    }
}
