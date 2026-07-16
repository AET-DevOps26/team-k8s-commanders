package com.caredesk.patient.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthServiceClientTest {

    private AuthServiceClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AuthServiceClient("http://unused");
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @Test
    void getUserByIdParsesAuthProfile() {
        UUID userId = UUID.randomUUID();
        server.expect(once(), requestTo("http://auth-service/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"%s","name":"Anna","email":"anna@example.com","role":"PATIENT"}
                        """.formatted(userId), MediaType.APPLICATION_JSON));

        var profile = client.getUserById(userId);

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(userId);
        assertThat(profile.getEmail()).isEqualTo("anna@example.com");
        server.verify();
    }

    @Test
    void getUserByIdReturnsNullForMissingOrBrokenResponse() {
        UUID missingId = UUID.randomUUID();
        UUID invalidId = UUID.randomUUID();
        server.expect(requestTo("http://auth-service/users/" + missingId))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://auth-service/users/" + invalidId))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThat(client.getUserById(missingId)).isNull();
        assertThat(client.getUserById(invalidId)).isNull();
        server.verify();
    }

    @Test
    void doctorDirectoryAndSpecializationsPreserveAuthResponses() {
        UUID doctorId = UUID.randomUUID();
        server.expect(requestTo(
                        "http://auth-service/internal/doctors?q=lee&specialization=Cardiology&page=0&size=20"))
                .andRespond(withSuccess("""
                        {"content":[{"id":"%s","name":"Dr. Lee","email":"lee@example.com","role":"DOCTOR"}],
                         "page":{"page":0,"size":20,"totalElements":1,"totalPages":1}}
                        """.formatted(doctorId), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://auth-service/internal/doctors/specializations"))
                .andRespond(withSuccess(
                        "[\"Cardiology\",\"General Medicine\"]", MediaType.APPLICATION_JSON));

        var doctors = client.searchDoctors("lee", "Cardiology", 0, 20);

        assertThat(doctors.getContent()).singleElement()
                .satisfies(doctor -> assertThat(doctor.getId()).isEqualTo(doctorId));
        assertThat(client.getSpecializations())
                .containsExactly("Cardiology", "General Medicine");
        server.verify();
    }

    @Test
    void doctorDirectoryFailureIsNotMisreportedAsEmptyResult() {
        server.expect(requestTo(
                        "http://auth-service/internal/doctors?q=&specialization=&page=0&size=20"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.searchDoctors("", "", 0, 20))
                .isInstanceOf(RuntimeException.class);
        server.verify();
    }
}
