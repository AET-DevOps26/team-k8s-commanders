package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import org.openapitools.api.DoctorsApi;
import org.openapitools.model.Schedule;
import org.openapitools.model.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controller for the {@code /doctors/**} endpoints.
 *
 * <p>Implements {@link DoctorsApi} and delegates business logic to
 * {@link DoctorService}. Authentication is enforced by the gateway-injected
 * {@code X-User-*} headers (see {@code PatientHeaderAuthFilter}). Per-role
 * ownership rules are tracked in issue #32.
 */
@Controller
public class DoctorsController implements DoctorsApi {

    private final DoctorService doctorService;

    /**
     * @param doctorService the read-only doctor query service
     */
    public DoctorsController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * Returns the doctor's profile view. Identity fields ({@code name},
     * {@code email}, {@code role}) live in auth-service and stay blank here,
     * the web client populates them from earlier auth-service responses.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return 200 with the doctor profile
     */
    @Override
    public ResponseEntity<UserProfile> getDoctorById(UUID doctorId) {
        return ResponseEntity.ok(doctorService.getProfile(doctorId));
    }

    /**
     * Returns the doctor's full schedule of bookable slots.
     *
     * @param doctorId the doctor's user id
     * @return 200 with the schedule, possibly with an empty slot list
     */
    @Override
    public ResponseEntity<Schedule> getDoctorSchedule(UUID doctorId) {
        return ResponseEntity.ok(doctorService.getSchedule(doctorId));
    }
}
