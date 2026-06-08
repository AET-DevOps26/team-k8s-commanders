package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import org.openapitools.api.DoctorsApi;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.openapitools.model.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
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

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final DoctorService doctorService;

    public DoctorsController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public ResponseEntity<UserProfile> getDoctorById(UUID doctorId) {
        return ResponseEntity.ok(doctorService.getProfile(doctorId));
    }

    @Override
    public ResponseEntity<Schedule> getDoctorSchedule(UUID doctorId) {
        return ResponseEntity.ok(doctorService.getSchedule(doctorId));
    }

    @Override
    public ResponseEntity<PaginatedUserProfileResponse> listDoctors(@Nullable String q,
                                                                     @Nullable String specialization,
                                                                     Integer page,
                                                                     Integer size) {
        int pageIndex = page != null ? page : DEFAULT_PAGE;
        int pageSize = size != null ? size : DEFAULT_SIZE;
        return ResponseEntity.ok(doctorService.listDoctors(q, specialization, pageIndex, pageSize));
    }
}
