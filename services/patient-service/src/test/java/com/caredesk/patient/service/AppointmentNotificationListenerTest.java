package com.caredesk.patient.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AppointmentNotificationListenerTest {

    @Test
    void committedEventIsForwardedWithoutChangingPayload() {
        NotificationServiceClient client = mock(NotificationServiceClient.class);
        AppointmentNotificationListener listener = new AppointmentNotificationListener(client);
        NotificationTriggerRequest request = new NotificationTriggerRequest(
                UUID.randomUUID(), UUID.randomUUID(), "patient@example.com",
                "CONFIRMATION", "Appointment confirmed", "You are booked.");

        listener.onAppointmentNotification(new AppointmentNotificationEvent(request));

        verify(client).notify(request);
    }
}
