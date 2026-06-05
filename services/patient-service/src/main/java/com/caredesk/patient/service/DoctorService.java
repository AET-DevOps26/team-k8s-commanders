package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorProfile;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorProfileRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorSlotRepository doctorSlotRepository;

    public DoctorService(DoctorProfileRepository doctorProfileRepository,
                         DoctorSlotRepository doctorSlotRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorSlotRepository = doctorSlotRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedUserProfileResponse listDoctors(@Nullable String q,
                                                    @Nullable String specialization,
                                                    int page,
                                                    int size) {
        Page<DoctorProfile> doctors = doctorProfileRepository.search(blankToEmpty(q), blankToEmpty(specialization),
                PageRequest.of(page, size));
        List<UserProfile> content = doctors.getContent().stream()
                .map(this::toProfile)
                .toList();
        PageMeta meta = new PageMeta(doctors.getNumber(), doctors.getSize(),
                doctors.getTotalElements(), doctors.getTotalPages());
        return new PaginatedUserProfileResponse(content, meta);
    }

    @Transactional(readOnly = true)
    public UserProfile getDoctor(UUID doctorId) {
        return doctorProfileRepository.findById(doctorId)
                .map(this::toProfile)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));
    }

    @Transactional(readOnly = true)
    public Schedule getSchedule(UUID doctorId) {
        if (!doctorProfileRepository.existsById(doctorId)) {
            throw new DoctorNotFoundException(doctorId);
        }
        List<ScheduleSlot> slots = doctorSlotRepository.findByDoctorId(doctorId).stream()
                .sorted(Comparator.comparing(DoctorSlot::getStartAt))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .map(slot -> new ScheduleSlot(slot.getStartAt(), slot.getEndAt(), slot.getAvailable()))
                .toList();
        return new Schedule(doctorId, slots);
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
