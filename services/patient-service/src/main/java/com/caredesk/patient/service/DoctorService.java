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
 * <p>Like {@link PatientService}, this service deliberately does not call out
 * to auth-service for the doctor's name / email / role. Those identity fields
 * live in the auth-service domain. The web client already has the booked
 * doctor's basic profile from earlier login or registration flows, and the
 * cross-service composition can be layered on later without changing this
 * API shape.
 */
@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorSlotRepository doctorSlotRepository;
    private final ScheduleSlotMapper scheduleSlotMapper;

    /**
     * @param doctorSlotRepository read access to the local doctor_slots table
     * @param scheduleSlotMapper   converts JPA slots into API DTOs
     */
    public DoctorService(DoctorSlotRepository doctorSlotRepository,
                         ScheduleSlotMapper scheduleSlotMapper) {
        this.doctorSlotRepository = doctorSlotRepository;
        this.scheduleSlotMapper = scheduleSlotMapper;
    }

    /**
     * Builds the doctor's profile view. Patient-service does not own the
     * identity fields ({@code name}, {@code email}, {@code role},
     * {@code specialization}, {@code licenseNumber}), so the response carries
     * only the requested id. A future cross-service lookup against
     * auth-service can populate the rest.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return a {@link UserProfile} with just the id field set
     */
    public UserProfile getProfile(UUID doctorId) {
        return new UserProfile().id(doctorId);
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
