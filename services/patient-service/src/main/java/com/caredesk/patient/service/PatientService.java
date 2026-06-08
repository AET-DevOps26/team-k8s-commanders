package com.caredesk.patient.service;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.Patient;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.PatientRepository;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.VisitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only profile, appointment listing and visit-history queries for a
 * patient.
 *
 * <p>This service deliberately does not call out to auth-service for the
 * caller's name / email / role. Those fields live in the auth-service domain
 * and the web client already has them from the login response. Cross-service
 * profile composition can be layered on later without changing this API
 * shape. For now the responses contain only the data the patient service
 * actually owns.
 */
@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final AuthServiceClient authServiceClient;

    /**
     * @param patientRepository     read access to the local patients table
     * @param appointmentRepository read access to the local appointments table
     * @param appointmentMapper     converts JPA appointments into API DTOs
     * @param authServiceClient     fetches identity fields from auth-service
     */
    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          AppointmentMapper appointmentMapper,
                          AuthServiceClient authServiceClient) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Builds the patient's profile view by combining the auth-service identity
     * fields ({@code name}, {@code email}, {@code role}) with the local
     * {@code patients} row (date of birth, phone number).
     *
     * <p>Falls back to an id-only profile if auth-service does not have the
     * user or cannot be reached, so the web client can still render a partial
     * state without a hard failure.
     *
     * @param patientId the patient's user id from auth-service
     * @return a {@link UserProfile} merged from both sources
     */
    public UserProfile getProfile(UUID patientId) {
        UserProfile profile = authServiceClient.getUserById(patientId);
        if (profile == null) {
            profile = new UserProfile().id(patientId);
        }
        Optional<Patient> patient = patientRepository.findById(patientId);
        UserProfile finalProfile = profile;
        patient.ifPresent(p -> {
            finalProfile.setDateOfBirth(p.getDateOfBirth());
            finalProfile.setPhoneNumber(p.getPhoneNumber());
        });
        return profile;
    }

    /**
     * Returns one page of appointments booked for the given patient.
     *
     * @param patientId the patient's user id
     * @param page      zero-based page index
     * @param size      page size, must be at least 1
     * @return a {@link PaginatedAppointmentResponse} with appointment DTOs and
     *         paging metadata
     */
    public PaginatedAppointmentResponse listAppointments(UUID patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> result = appointmentRepository.findByPatientId(patientId, pageable);
        List<org.openapitools.model.Appointment> content = result.getContent().stream()
                .map(appointmentMapper::toApi)
                .toList();
        PageMeta meta = new PageMeta()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
        return new PaginatedAppointmentResponse(content, meta);
    }

    /**
     * Returns the patient's visit history.
     *
     * <p>For now the history is composed only of appointments owned by the
     * patient service. Clinical notes belong to a separate service that does
     * not exist yet, so {@link VisitHistory#getNotes()} is always empty.
     *
     * @param patientId the patient's user id
     * @return a {@link VisitHistory} with appointments and an empty notes list
     */
    public VisitHistory getVisitHistory(UUID patientId) {
        List<org.openapitools.model.Appointment> appointments = appointmentRepository
                .findByPatientId(patientId).stream()
                .map(appointmentMapper::toApi)
                .toList();
        return new VisitHistory(patientId, appointments);
    }
}
