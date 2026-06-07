package com.caredesk.patient.service;

import com.caredesk.patient.repository.DoctorSlotRepository;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read-only profile and schedule queries for a doctor.
 *
 * <p>{@code getProfile} fetches the doctor's identity fields from
 * auth-service via {@link AuthServiceClient} so the response is contract
 * compliant. If the auth-service lookup fails (404 or transport error) the
 * service falls back to an id-only response rather than failing the whole
 * request.
 */
@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorSlotRepository doctorSlotRepository;
    private final ScheduleSlotMapper scheduleSlotMapper;
    private final AuthServiceClient authServiceClient;

    /**
     * @param doctorSlotRepository read access to the local doctor_slots table
     * @param scheduleSlotMapper   converts JPA slots into API DTOs
     * @param authServiceClient    fetches identity fields from auth-service
     */
    public DoctorService(DoctorSlotRepository doctorSlotRepository,
                         ScheduleSlotMapper scheduleSlotMapper,
                         AuthServiceClient authServiceClient) {
        this.doctorSlotRepository = doctorSlotRepository;
        this.scheduleSlotMapper = scheduleSlotMapper;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Builds the doctor's profile view by combining the auth-service identity
     * fields ({@code name}, {@code email}, {@code role}, etc.) with any
     * doctor-specific data held locally. Falls back to an id-only profile if
     * auth-service does not have the user or cannot be reached.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return a populated {@link UserProfile}
     */
    public UserProfile getProfile(UUID doctorId) {
        UserProfile profile = authServiceClient.getUserById(doctorId);
        if (profile == null) {
            profile = new UserProfile().id(doctorId);
        }
        return profile;
    }

    /**
     * Returns the doctor's complete schedule of bookable slots, in no
     * specific order.
     *
     * @param doctorId the doctor's user id
     * @return a {@link Schedule} for that doctor, possibly with an empty
     *         slot list
     */
    public Schedule getSchedule(UUID doctorId) {
        List<ScheduleSlot> slots = doctorSlotRepository.findByDoctorId(doctorId).stream()
                .map(scheduleSlotMapper::toApi)
                .toList();
        return new Schedule(doctorId, slots);
    }
}
