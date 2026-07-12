package com.caredesk.patient.service;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.repository.AppointmentRepository;
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
import java.util.UUID;

/**
 * Read-only profile, appointment listing and visit-history queries for a
 * patient.
 *
 * <p>Identity is owned by auth-service and read through {@link AuthServiceClient};
 * this service owns only the appointment data keyed by the auth user id.
 */
@Service
@Transactional(readOnly = true)
public class PatientService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final AuthServiceClient authServiceClient;

    /**
     * @param appointmentRepository read access to the local appointments table
     * @param appointmentMapper     converts JPA appointments into API DTOs
     * @param authServiceClient     fetches identity fields from auth-service
     */
    public PatientService(AppointmentRepository appointmentRepository,
                          AppointmentMapper appointmentMapper,
                          AuthServiceClient authServiceClient) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Returns the patient's profile from auth-service, which owns all identity
     * fields including date of birth and phone number. Falls back to an id-only
     * profile if auth-service does not have the user or cannot be reached, so
     * the web client can still render a partial state without a hard failure.
     *
     * @param patientId the patient's user id from auth-service
     * @return the {@link UserProfile} owned by auth-service
     */
    public UserProfile getProfile(UUID patientId) {
        UserProfile profile = authServiceClient.getUserById(patientId);
        if (profile == null) {
            profile = new UserProfile().id(patientId);
        }
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
     * Returns the patient's visit history, composed of the appointments owned by
     * this service. Clinical notes are owned by the separate notes-service and
     * are not part of the patient-facing visit history.
     *
     * @param patientId the patient's user id
     * @return a {@link VisitHistory} with the patient's appointments
     */
    public VisitHistory getVisitHistory(UUID patientId) {
        List<org.openapitools.model.Appointment> appointments = appointmentRepository
                .findByPatientId(patientId).stream()
                .map(appointmentMapper::toApi)
                .toList();
        return new VisitHistory(patientId, appointments);
    }
}
