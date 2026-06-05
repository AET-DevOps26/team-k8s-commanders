package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import java.util.UUID;
import org.openapitools.api.DoctorsApi;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.openapitools.model.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

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
        return ResponseEntity.ok(doctorService.getDoctor(doctorId));
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
