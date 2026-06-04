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

    /**
     * @param patientRepository     read access to the local patients table
     * @param appointmentRepository read access to the local appointments table
     * @param appointmentMapper     converts JPA appointments into API DTOs
     */
    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          AppointmentMapper appointmentMapper) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
    }

    /**
     * Builds the patient's profile view from the local {@code patients} row.
     *
     * <p>If no row exists the response still carries the requested {@code id}
     * with all optional fields left blank, so the web client can render a
     * "complete your profile" state without a 404 round-trip.
     *
     * @param patientId the patient's user id from auth-service
     * @return a {@link UserProfile} populated from the local patient row
     */
    public UserProfile getProfile(UUID patientId) {
        UserProfile profile = new UserProfile().id(patientId);
        Optional<Patient> patient = patientRepository.findById(patientId);
        patient.ifPresent(p -> {
            profile.setDateOfBirth(p.getDateOfBirth());
            profile.setPhoneNumber(p.getPhoneNumber());
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
