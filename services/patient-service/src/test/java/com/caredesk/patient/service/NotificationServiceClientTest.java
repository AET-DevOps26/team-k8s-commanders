package com.caredesk.patient.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit test for {@link NotificationServiceClient}'s graceful degradation.
 */
class NotificationServiceClientTest {

    @Test
    void notify_swallowsErrorsWhenNotificationServiceUnreachable() {
        // Port 1 is not listening, so the call fails — it must not propagate, so
        // a down notification-service can never break booking / reschedule / cancel.
        NotificationServiceClient client = new NotificationServiceClient("http://localhost:1");

        NotificationTriggerRequest request = new NotificationTriggerRequest(
                UUID.randomUUID(), UUID.randomUUID(), "anna@example.com",
                "CONFIRMATION", "Appointment confirmed", "You're booked.");

        assertThatCode(() -> client.notify(request)).doesNotThrowAnyException();
    }
}
