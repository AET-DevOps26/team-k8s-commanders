package com.caredesk.patient.controller;

import com.caredesk.patient.service.Caller;
import com.caredesk.patient.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.api.PatientsApi;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserProfile;
import org.openapitools.model.VisitHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controller for the {@code /patients/**} endpoints.
 *
 * <p>Implements {@link PatientsApi} and delegates business logic to
 * {@link PatientService}. Authentication is enforced via the gateway-injected
 * {@code X-User-*} headers (see {@code SecurityConfig}); on top of that, every
 * endpoint here is scoped to the patient themself or clinic staff — a patient
 * can never read another patient's profile, appointments or visit history
 * (issue #172).
 */
@Controller
public class PatientsController implements PatientsApi {

    private final PatientService patientService;
    private final HttpServletRequest request;

    /**
     * @param patientService the read-only query service backing these endpoints
     * @param request        request-scoped proxy used to read the trusted
     *                       {@code X-User-*} headers for the current call
     */
    public PatientsController(PatientService patientService, HttpServletRequest request) {
        this.patientService = patientService;
        this.request = request;
    }

    /**
     * Returns the patient-owned slice of a user profile (date of birth and
     * phone number). The {@code name}, {@code email} and {@code role} fields
     * live in auth-service and are intentionally left blank here.
     *
     * @param patientId the patient's user id from auth-service
     * @return 200 with the patient profile, 403 if the caller is another patient
     */
    @Override
    public ResponseEntity<UserProfile> getPatientById(UUID patientId) {
        requireSelfOrStaff(patientId);
        return ResponseEntity.ok(patientService.getProfile(patientId));
    }

    /**
     * Lists appointments booked for the patient.
     *
     * @param patientId the patient's user id
     * @param page      zero-based page index, defaulted to 0 by the API
     * @param size      page size, defaulted to 20 by the API
     * @return 200 with the page of appointments, 403 if the caller is another patient
     */
    @Override
    public ResponseEntity<PaginatedAppointmentResponse> listPatientAppointments(
            UUID patientId, Integer page, Integer size) {
        requireSelfOrStaff(patientId);
        return ResponseEntity.ok(patientService.listAppointments(patientId, page, size));
    }

    /**
     * Returns the patient's visit history (appointments plus clinical notes,
     * composed with the notes-service).
     *
     * @param patientId the patient's user id
     * @return 200 with the visit history, 403 if the caller is another patient
     */
    @Override
    public ResponseEntity<VisitHistory> getPatientVisitHistory(UUID patientId) {
        requireSelfOrStaff(patientId);
        return ResponseEntity.ok(patientService.getVisitHistory(patientId));
    }

    /**
     * Rejects callers that are neither the patient themself nor clinic staff.
     * Doctors legitimately read patient records (the patient-records workspace
     * and the AI assistant's grounding both go through these endpoints), so the
     * {@code DOCTOR} and {@code ADMIN} roles pass; any other patient is denied.
     *
     * @param patientId the patient whose data is being requested
     * @throws AccessDeniedException if the caller may not read this patient's data
     */
    private void requireSelfOrStaff(UUID patientId) {
        Caller caller = GatewayIdentity.caller(request);
        if (caller.is(patientId) || caller.isDoctor() || caller.isAdmin()) {
            return;
        }
        throw new AccessDeniedException("Not your patient record");
    }
}
