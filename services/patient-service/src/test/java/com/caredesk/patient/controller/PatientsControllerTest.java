package com.caredesk.patient.controller;

import com.caredesk.patient.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.openapitools.model.VisitHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the self-or-staff ownership rule on the {@code /patients/**}
 * endpoints (issue #172).
 */
class PatientsControllerTest {

    private final PatientService patientService = mock(PatientService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final PatientsController controller = new PatientsController(patientService, request);

    private void callerIs(UUID userId, UserRole role) {
        when(request.getHeader("X-User-Id")).thenReturn(userId.toString());
        when(request.getHeader("X-User-Role")).thenReturn(role.name());
    }

    @Test
    void patientCanReadTheirOwnProfileAppointmentsAndHistory() {
        UUID patientId = UUID.randomUUID();
        callerIs(patientId, UserRole.PATIENT);
        when(patientService.getProfile(patientId))
                .thenReturn(new UserProfile(patientId, null, null, UserRole.PATIENT));
        when(patientService.listAppointments(patientId, 0, 20))
                .thenReturn(new PaginatedAppointmentResponse(List.of(), new PageMeta(0, 20, 0L, 0)));
        when(patientService.getVisitHistory(patientId))
                .thenReturn(new VisitHistory(patientId, List.of()));

        assertThat(controller.getPatientById(patientId).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(controller.listPatientAppointments(patientId, 0, 20).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(controller.getPatientVisitHistory(patientId).getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void patientCannotReadAnotherPatientsData() {
        UUID victim = UUID.randomUUID();
        callerIs(UUID.randomUUID(), UserRole.PATIENT);

        assertThatThrownBy(() -> controller.getPatientById(victim))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.listPatientAppointments(victim, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.getPatientVisitHistory(victim))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(patientService);
    }

    @Test
    void doctorCanReadPatientRecords() {
        UUID patientId = UUID.randomUUID();
        callerIs(UUID.randomUUID(), UserRole.DOCTOR);
        when(patientService.getVisitHistory(patientId))
                .thenReturn(new VisitHistory(patientId, List.of()));

        ResponseEntity<VisitHistory> result = controller.getPatientVisitHistory(patientId);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void adminCanReadPatientRecords() {
        UUID patientId = UUID.randomUUID();
        callerIs(UUID.randomUUID(), UserRole.ADMIN);
        when(patientService.getProfile(patientId))
                .thenReturn(new UserProfile(patientId, null, null, UserRole.PATIENT));

        assertThat(controller.getPatientById(patientId).getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void missingIdentityHeaderIsRejectedAsUnauthorized() {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("PATIENT");

        assertThatThrownBy(() -> controller.getPatientById(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
        verifyNoInteractions(patientService);
    }
}
