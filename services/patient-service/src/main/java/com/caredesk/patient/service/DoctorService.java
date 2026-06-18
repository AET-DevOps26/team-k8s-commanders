package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorProfile;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorProfileRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Read-only doctor search, profile and schedule queries.
 *
 * <p>{@code listDoctors} searches the local {@code doctor_profiles} table.
 * {@code getProfile} fetches identity fields from auth-service via
 * {@link AuthServiceClient} and falls back to an id-only response when the
 * lookup fails. {@code getSchedule} returns available slots sorted by start
 * time. Doctors can also publish new available slots without assigning a
 * patient.
 */
@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final ScheduleSlotMapper scheduleSlotMapper;
    private final AuthServiceClient authServiceClient;

    public DoctorService(DoctorProfileRepository doctorProfileRepository,
                         DoctorSlotRepository doctorSlotRepository,
                         ScheduleSlotMapper scheduleSlotMapper,
                         AuthServiceClient authServiceClient) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorSlotRepository = doctorSlotRepository;
        this.scheduleSlotMapper = scheduleSlotMapper;
        this.authServiceClient = authServiceClient;
    }

    public PaginatedUserProfileResponse listDoctors(@Nullable String q,
                                                    @Nullable String specialization,
                                                    int page,
                                                    int size) {
        Page<DoctorProfile> doctors = doctorProfileRepository.search(
                blankToEmpty(q), blankToEmpty(specialization), PageRequest.of(page, size));
        List<UserProfile> content = doctors.getContent().stream()
                .map(this::toProfile)
                .toList();
        PageMeta meta = new PageMeta(doctors.getNumber(), doctors.getSize(),
                doctors.getTotalElements(), doctors.getTotalPages());
        return new PaginatedUserProfileResponse(content, meta);
    }

    public UserProfile getProfile(UUID doctorId) {
        UserProfile profile = authServiceClient.getUserById(doctorId);
        if (profile == null) {
            profile = new UserProfile().id(doctorId);
        }
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

        doctorProfileRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));

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

    private UserProfile toProfile(DoctorProfile doctor) {
        UserProfile profile = new UserProfile(doctor.getId(), doctor.getName(), doctor.getEmail(), UserRole.DOCTOR);
        profile.setSpecialization(doctor.getSpecialization());
        profile.setLicenseNumber(doctor.getLicenseNumber());
        profile.setClinicId(doctor.getClinicId());
        return profile;
    }

    private String blankToEmpty(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
