package com.caredesk.patient.service;

import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Thin HTTP client for auth-service's internal {@code GET /users/{id}}
 * endpoint.
 *
 * <p>Used by {@link PatientService} and {@link DoctorService} to fetch the
 * identity fields ({@code name}, {@code email}, {@code role}) that
 * auth-service owns, so the patient and doctor profile responses are fully
 * populated rather than id-only.
 *
 * <p>Calls bypass the API gateway and go directly to auth-service inside the
 * compose network. The configured {@code AUTH_SERVICE_URL} therefore points
 * at the internal service hostname, not the public gateway.
 */
@Component
public class AuthServiceClient {

    private final RestClient restClient;

    /**
     * @param baseUrl the internal auth-service base URL, e.g.
     *                {@code http://auth-service:8081}
     */
    public AuthServiceClient(@Value("${auth-service.url:http://localhost:8081}") String baseUrl) {
        // Explicit timeouts so a hung auth-service can't block write transactions
        // (e.g. DoctorService.requireDoctor() during schedule-slot creation).
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * Fetches a user profile from auth-service.
     *
     * @param userId the user id
     * @return the user profile, or {@code null} if auth-service returned 404
     *         or could not be reached
     */
    public UserProfile getUserById(UUID userId) {
        try {
            return restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(UserProfile.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RuntimeException e) {
            // Network errors, timeouts, etc. Caller falls back to id-only profile.
            return null;
        }
    }

    /**
     * Searches enabled doctors owned by auth-service. Unlike {@link #getUserById},
     * failures are not swallowed: an empty list is indistinguishable from "no
     * doctors", so the error propagates and the listing surfaces as 5xx.
     */
    public PaginatedUserProfileResponse searchDoctors(String q, String specialization, int page, int size) {
        return restClient.get()
                .uri("/internal/doctors?q={q}&specialization={specialization}&page={page}&size={size}",
                        q, specialization, page, size)
                .retrieve()
                .body(PaginatedUserProfileResponse.class);
    }

    /**
     * Fetches the distinct doctor specializations from auth-service. As with
     * {@link #searchDoctors}, failures are not swallowed — an empty list would
     * be indistinguishable from a real error, so it propagates as a 5xx.
     */
    public List<String> getSpecializations() {
        return restClient.get()
                .uri("/internal/doctors/specializations")
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});
    }
}
