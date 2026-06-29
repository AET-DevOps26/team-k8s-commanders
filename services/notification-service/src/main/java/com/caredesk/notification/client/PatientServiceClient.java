package com.caredesk.notification.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Thin HTTP client for patient-service's internal upcoming-appointments feed.
 *
 * <p>Used by the reminder scheduler to discover appointments due soon. Calls
 * go directly to patient-service inside the cluster network (bypassing the
 * gateway), so {@code PATIENT_SERVICE_URL} points at the internal service
 * hostname. Failures are swallowed and returned as an empty list — a reminder
 * scan that can't reach patient-service simply does nothing this tick.
 */
@Component
public class PatientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceClient.class);

    private static final ParameterizedTypeReference<List<UpcomingAppointment>> LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * @param baseUrl the internal patient-service base URL, e.g.
     *                {@code http://patient-service:8082}
     */
    public PatientServiceClient(@Value("${patient-service.url:http://localhost:8082}") String baseUrl) {
        // Explicit timeouts so a hung patient-service can't block the scheduler
        // thread and stall future reminder scans.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * Fetches appointments starting within the next {@code withinHours} hours.
     *
     * @param withinHours the look-ahead window in hours
     * @return the upcoming appointments, or an empty list if patient-service
     *         could not be reached
     */
    public List<UpcomingAppointment> fetchUpcoming(int withinHours) {
        try {
            List<UpcomingAppointment> result = restClient.get()
                    .uri("/internal/appointments/upcoming?withinHours={h}", withinHours)
                    .retrieve()
                    .body(LIST_TYPE);
            return result != null ? result : List.of();
        } catch (RuntimeException e) {
            log.error("Failed to fetch upcoming appointments from patient-service: {}", e.getMessage());
            return List.of();
        }
    }
}
