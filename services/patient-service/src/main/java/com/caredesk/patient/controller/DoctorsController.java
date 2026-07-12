package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.api.DoctorsApi;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.RecurringScheduleCreate;
import org.openapitools.model.RecurringScheduleResult;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Controller for the {@code /doctors/**} endpoints.
 *
 * <p>Implements {@link DoctorsApi} and delegates business logic to
 * {@link DoctorService}. Authentication is enforced by the gateway-injected
 * {@code X-User-*} headers (see {@code PatientHeaderAuthFilter}). Slot
 * creation is scoped to the authenticated doctor, while admins may create for
 * any doctor.
 */
@Controller
public class DoctorsController implements DoctorsApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_DOCTOR = "DOCTOR";

    private final DoctorService doctorService;
    private final HttpServletRequest request;

    public DoctorsController(DoctorService doctorService, HttpServletRequest request) {
        this.doctorService = doctorService;
        this.request = request;
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
    public ResponseEntity<ScheduleSlot> createDoctorScheduleSlot(UUID doctorId,
                                                                  ScheduleSlotCreate scheduleSlotCreate) {
        boolean actingAsAdmin = requireOwnDoctorOrAdmin(doctorId);
        if (actingAsAdmin) {
            doctorService.verifyDoctorExists(doctorId);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.createScheduleSlot(doctorId, scheduleSlotCreate));
    }

    @Override
    public ResponseEntity<RecurringScheduleResult> createDoctorRecurringSchedule(UUID doctorId,
                                                                                  RecurringScheduleCreate recurringScheduleCreate) {
        boolean actingAsAdmin = requireOwnDoctorOrAdmin(doctorId);
        if (actingAsAdmin) {
            doctorService.verifyDoctorExists(doctorId);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.createRecurringScheduleSlots(doctorId, recurringScheduleCreate));
    }

    @Override
    public ResponseEntity<Void> deleteDoctorScheduleSlot(UUID doctorId, UUID slotId) {
        // No verifyDoctorExists for admins: the doctor-scoped lookup already
        // yields 404 when the slot does not belong to a real doctor.
        requireOwnDoctorOrAdmin(doctorId);
        doctorService.deleteScheduleSlot(doctorId, slotId);
        return ResponseEntity.noContent().build();
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

    @Override
    public ResponseEntity<List<String>> listDoctorSpecializations() {
        return ResponseEntity.ok(doctorService.listSpecializations());
    }

    private boolean requireOwnDoctorOrAdmin(UUID doctorId) {
        String role = normalise(request.getHeader(ROLE_HEADER));

        if (ROLE_ADMIN.equals(role)) {
            return true;
        }

        if (!ROLE_DOCTOR.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor role required");
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing doctor identity");
        }

        UUID currentDoctorId;
        try {
            currentDoctorId = UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid doctor identity", exception);
        }

        if (!doctorId.equals(currentDoctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctors can only manage their own schedule");
        }
        return false;
    }

    private String normalise(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
