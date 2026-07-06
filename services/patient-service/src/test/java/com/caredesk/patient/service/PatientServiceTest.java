package com.caredesk.patient.service;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.openapitools.model.AppointmentStatus;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.VisitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PatientService}. Backed by mocked collaborators so the
 * tests do not need a database.
 */
class PatientServiceTest {

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final AppointmentMapper appointmentMapper = new AppointmentMapper();
    private final AuthServiceClient authServiceClient = mock(AuthServiceClient.class);
    private final PatientService service = new PatientService(
            appointmentRepository, appointmentMapper, authServiceClient);

    @Test
    void getProfile_returnsAuthServiceIdentity() {
        UUID id = UUID.randomUUID();
        UserProfile authProfile = new UserProfile(id, "Alice", "alice@x.com",
                org.openapitools.model.UserRole.PATIENT);
        authProfile.setDateOfBirth(LocalDate.of(1990, 1, 15));
        authProfile.setPhoneNumber("+44 20 1234 5678");
        when(authServiceClient.getUserById(id)).thenReturn(authProfile);

        UserProfile profile = service.getProfile(id);

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getName()).isEqualTo("Alice");
        assertThat(profile.getEmail()).isEqualTo("alice@x.com");
        assertThat(profile.getRole()).isEqualTo(org.openapitools.model.UserRole.PATIENT);
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(profile.getPhoneNumber()).isEqualTo("+44 20 1234 5678");
    }

    @Test
    void getProfile_fallsBackToIdOnly_whenAuthServiceMisses() {
        UUID id = UUID.randomUUID();
        when(authServiceClient.getUserById(id)).thenReturn(null);

        UserProfile profile = service.getProfile(id);

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getName()).isNull();
    }

    @Test
    void listAppointments_returnsPagedResponse() {
        UUID patientId = UUID.randomUUID();
        Appointment a1 = appointment(patientId);
        Appointment a2 = appointment(patientId);
        Page<Appointment> page = new PageImpl<>(List.of(a1, a2), PageRequest.of(0, 20), 2);
        when(appointmentRepository.findByPatientId(eq(patientId), any())).thenReturn(page);

        PaginatedAppointmentResponse response = service.listAppointments(patientId, 0, 20);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getPatientId()).isEqualTo(patientId);
        assertThat(response.getPage().getPage()).isZero();
        assertThat(response.getPage().getSize()).isEqualTo(20);
        assertThat(response.getPage().getTotalElements()).isEqualTo(2);
        assertThat(response.getPage().getTotalPages()).isEqualTo(1);
    }

    @Test
    void listAppointments_returnsEmptyPage_whenNoAppointments() {
        UUID patientId = UUID.randomUUID();
        when(appointmentRepository.findByPatientId(eq(patientId), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PaginatedAppointmentResponse response = service.listAppointments(patientId, 0, 20);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPage().getTotalElements()).isZero();
        assertThat(response.getPage().getTotalPages()).isZero();
    }

    @Test
    void getVisitHistory_wrapsAppointments_andLeavesNotesEmpty() {
        UUID patientId = UUID.randomUUID();
        Appointment a1 = appointment(patientId);
        when(appointmentRepository.findByPatientId(patientId)).thenReturn(List.of(a1));

        VisitHistory history = service.getVisitHistory(patientId);

        assertThat(history.getPatientId()).isEqualTo(patientId);
        assertThat(history.getAppointments()).hasSize(1);
        assertThat(history.getNotes()).isEmpty();
    }

    private static Appointment appointment(UUID patientId) {
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setPatientId(patientId);
        a.setDoctorId(UUID.randomUUID());
        a.setDateTime(OffsetDateTime.now());
        a.setStatus(AppointmentStatus.SCHEDULED);
        a.setDuration(30);
        return a;
    }
}
