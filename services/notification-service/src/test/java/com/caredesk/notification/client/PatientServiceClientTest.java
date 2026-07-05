package com.caredesk.notification.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit test for {@link PatientServiceClient}'s graceful degradation.
 */
class PatientServiceClientTest {

    @Test
    void fetchUpcoming_returnsEmptyWhenPatientServiceUnreachable() {
        // Port 1 is not listening, so the call fails — the scheduler should then
        // simply do nothing this tick rather than blow up.
        PatientServiceClient client = new PatientServiceClient("http://localhost:1");

        assertThatCode(() -> assertThat(client.fetchUpcoming(24)).isEmpty())
                .doesNotThrowAnyException();
    }
}
