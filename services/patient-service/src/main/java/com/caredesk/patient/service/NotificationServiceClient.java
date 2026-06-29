package com.caredesk.patient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Thin HTTP client that asks notification-service to record and deliver an
 * appointment notification.
 *
 * <p>Calls go directly to notification-service's internal endpoint inside the
 * cluster network (bypassing the gateway), so {@code NOTIFICATION_SERVICE_URL}
 * points at the internal service hostname.
 *
 * <p>Every call is best-effort: any failure (notification-service down,
 * timeout, non-2xx) is logged and swallowed. Booking, rescheduling and
 * cancelling must never fail because a notification could not be dispatched.
 */
@Component
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestClient restClient;

    /**
     * @param baseUrl the internal notification-service base URL, e.g.
     *                {@code http://notification-service:8084}
     */
    public NotificationServiceClient(
            @Value("${notification-service.url:http://localhost:8084}") String baseUrl) {
        // Explicit timeouts so a hung notification-service can't stall booking,
        // rescheduling or cancellation before the failure is swallowed.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * Fires a notification trigger, swallowing any failure.
     *
     * @param request the trigger payload
     */
    public void notify(NotificationTriggerRequest request) {
        try {
            restClient.post()
                    .uri("/internal/notifications")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            // Best-effort: never propagate, so the booking flow is not blocked.
            log.error("Failed to trigger notification for appointment {}: {}",
                    request.appointmentId(), e.getMessage());
        }
    }
}
