package com.caredesk.patient.service;

import org.openapitools.model.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

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
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
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
}
