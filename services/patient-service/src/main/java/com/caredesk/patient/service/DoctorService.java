package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Doctor directory and schedule queries.
 *
 * <p>Doctor identity is owned by auth-service: {@code listDoctors} and
 * {@code getProfile} read through {@link AuthServiceClient}, while this service
 * owns only the scheduling data ({@code doctor_slots}) keyed by the auth user
 * id. Doctors publish available slots without assigning a patient.
 */
@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorSlotRepository doctorSlotRepository;
    private final ScheduleSlotMapper scheduleSlotMapper;
    private final AuthServiceClient authServiceClient;

    public DoctorService(DoctorSlotRepository doctorSlotRepository,
                         ScheduleSlotMapper scheduleSlotMapper,
                         AuthServiceClient authServiceClient) {
        this.doctorSlotRepository = doctorSlotRepository;
        this.scheduleSlotMapper = scheduleSlotMapper;
        this.authServiceClient = authServiceClient;
    }

    public PaginatedUserProfileResponse listDoctors(@Nullable String q,
                                                    @Nullable String specialization,
                                                    int page,
                                                    int size) {
        return authServiceClient.searchDoctors(blankToEmpty(q), blankToEmpty(specialization), page, size);
    }

    public UserProfile getProfile(UUID doctorId) {
        UserProfile profile = authServiceClient.getUserById(doctorId);
        if (profile == null) {
            return new UserProfile().id(doctorId);
        }
        profile.setPhoneNumber(null);
        profile.setDateOfBirth(null);
        return profile;
    }

    public Schedule getSchedule(UUID doctorId) {
        List<ScheduleSlot> slots = doctorSlotRepository.findByDoctorId(doctorId).stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .sorted(Comparator.comparing(DoctorSlot::getStartAt))
                .map(scheduleSlotMapper::toApi)
                .toList();
        return new Schedule(doctorId, slots);
    }

    /**
     * Verifies an id belongs to a doctor in auth-service. Only needed when an
     * admin creates a slot for another user; a doctor acting on their own
     * schedule is already proven by the gateway-injected role header.
     */
    public void verifyDoctorExists(UUID doctorId) {
        UserProfile profile = authServiceClient.getUserById(doctorId);
        if (profile == null || profile.getRole() != UserRole.DOCTOR) {
            throw new DoctorNotFoundException(doctorId);
        }
    }

    @Transactional
    public ScheduleSlot createScheduleSlot(UUID doctorId, ScheduleSlotCreate request) {
        if (request == null || request.getStartAt() == null || request.getEndAt() == null) {
            throw new IllegalArgumentException("Schedule slot startAt and endAt are required");
        }

        OffsetDateTime startAt = request.getStartAt();
        OffsetDateTime endAt = request.getEndAt();

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Schedule slot endAt must be after startAt");
        }

        if (startAt.isBefore(OffsetDateTime.now())) {
            throw new AppointmentStateConflictException("Past schedule slots cannot be created");
        }

        doctorSlotRepository.lockForSlotWrite(doctorId);

        if (doctorSlotRepository.existsOverlappingSlot(doctorId, startAt, endAt)) {
            throw new AppointmentStateConflictException("Schedule slot overlaps an existing slot");
        }

        DoctorSlot slot = new DoctorSlot();
        slot.setDoctorId(doctorId);
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        slot.setAvailable(true);

        return scheduleSlotMapper.toApi(doctorSlotRepository.save(slot));
    }

    private String blankToEmpty(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
