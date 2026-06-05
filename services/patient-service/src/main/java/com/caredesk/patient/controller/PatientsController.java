package com.caredesk.patient.controller;

import com.caredesk.patient.service.PatientService;
import org.openapitools.api.PatientsApi;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.VisitHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controller for the {@code /patients/**} endpoints.
 *
 * <p>Implements {@link PatientsApi} and delegates business logic to
 * {@link PatientService}. Endpoints currently require the caller to be
 * authenticated via the gateway-injected {@code X-User-*} headers (enforced
 * by Spring Security in {@code SecurityConfig}). Per-role ownership checks
 * (a {@code PATIENT} may only view themselves) are a follow up tracked in
 * issue #32.
 */
@Controller
public class PatientsController implements PatientsApi {

    private final PatientService patientService;

    /**
     * @param patientService the read-only query service backing these endpoints
     */
    public PatientsController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Returns the patient-owned slice of a user profile (date of birth and
     * phone number). The {@code name}, {@code email} and {@code role} fields
     * live in auth-service and are intentionally left blank here.
     *
     * @param patientId the patient's user id from auth-service
     * @return 200 with the patient profile
     */
    @Override
    public ResponseEntity<UserProfile> getPatientById(UUID patientId) {
        return ResponseEntity.ok(patientService.getProfile(patientId));
    }

    /**
     * Lists appointments booked for the patient.
     *
     * @param patientId the patient's user id
     * @param page      zero-based page index, defaulted to 0 by the API
     * @param size      page size, defaulted to 20 by the API
     * @return 200 with the page of appointments
     */
    @Override
    public ResponseEntity<PaginatedAppointmentResponse> listPatientAppointments(
            UUID patientId, Integer page, Integer size) {
        return ResponseEntity.ok(patientService.listAppointments(patientId, page, size));
    }

    /**
     * Returns the patient's visit history, currently composed of appointments
     * only. Clinical notes will be filled in once a clinical-notes service
     * exists.
     *
     * @param patientId the patient's user id
     * @return 200 with the visit history
     */
    @Override
    public ResponseEntity<VisitHistory> getPatientVisitHistory(UUID patientId) {
        return ResponseEntity.ok(patientService.getVisitHistory(patientId));
    }
}
