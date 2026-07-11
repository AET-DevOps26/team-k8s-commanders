package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.RecurringScheduleCreate;
import org.openapitools.model.RecurringScheduleResult;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.SlotInterval;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.openapitools.model.Weekday;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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

    /** Recurring availability may extend at most 12 weeks past its start date. */
    private static final int MAX_HORIZON_DAYS = 84;
    /** Upper bound on slots a single recurring request may materialize. */
    private static final int MAX_GENERATED_SLOTS = 1000;

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

    /**
     * Distinct doctor specializations, delegated to auth-service (the owner of
     * doctor identity). Backs the booking flow's specialization dropdown.
     */
    public List<String> listSpecializations() {
        return authServiceClient.getSpecializations();
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

    /**
     * Expands a weekly recurrence into individual slots. Candidates that
     * overlap an existing slot are skipped and reported in the result rather
     * than failing the request; occurrences already in the past are silently
     * dropped so a range starting "this week" works mid-week. Boundaries are
     * computed as wall-clock times in the requested zone, so the daily window
     * stays fixed across DST transitions.
     */
    @Transactional
    public RecurringScheduleResult createRecurringScheduleSlots(UUID doctorId, RecurringScheduleCreate request) {
        if (request == null) {
            throw new IllegalArgumentException("Recurring schedule request body is required");
        }

        Set<Weekday> weekdays = request.getWeekdays();
        if (weekdays == null || weekdays.isEmpty()) {
            throw new IllegalArgumentException("At least one weekday is required");
        }

        ZoneId zone = parseZone(request.getTimezone());
        LocalTime windowStart = parseTime(request.getStartTime(), "startTime");
        LocalTime windowEnd = parseTime(request.getEndTime(), "endTime");
        if (!windowEnd.isAfter(windowStart)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        if (request.getSlotDurationMinutes() == null) {
            throw new IllegalArgumentException("slotDurationMinutes is required");
        }
        int duration = request.getSlotDurationMinutes().getValue();
        long windowMinutes = Duration.between(windowStart, windowEnd).toMinutes();
        if (windowMinutes < duration) {
            throw new IllegalArgumentException("Time window is shorter than one slot");
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (endDate.isAfter(startDate.plusDays(MAX_HORIZON_DAYS))) {
            throw new IllegalArgumentException("Recurring availability may cover at most 12 weeks");
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        weekdays.forEach(weekday -> days.add(DayOfWeek.valueOf(weekday.getValue())));

        OffsetDateTime now = OffsetDateTime.now();
        doctorSlotRepository.lockForSlotWrite(doctorId);

        List<DoctorSlot> toCreate = new ArrayList<>();
        List<SlotInterval> skipped = new ArrayList<>();
        OffsetDateTime lastAcceptedEnd = null;
        int candidateCount = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!days.contains(date.getDayOfWeek())) {
                continue;
            }
            LocalTime time = windowStart;
            while (true) {
                LocalTime slotEndTime = time.plusMinutes(duration);
                // Wrapped past midnight (slotEndTime < time) or past the window.
                if (!slotEndTime.isAfter(time) || slotEndTime.isAfter(windowEnd)) {
                    break;
                }
                OffsetDateTime slotStart = date.atTime(time).atZone(zone).toOffsetDateTime();
                OffsetDateTime slotEnd = date.atTime(slotEndTime).atZone(zone).toOffsetDateTime();
                time = slotEndTime;
                if (!slotEnd.isAfter(slotStart)) {
                    continue; // degenerate range inside a DST gap
                }
                if (!slotStart.isAfter(now)) {
                    continue; // past occurrence: neither created nor reported
                }
                if (++candidateCount > MAX_GENERATED_SLOTS) {
                    throw new IllegalArgumentException(
                            "Recurring availability would create more than " + MAX_GENERATED_SLOTS + " slots");
                }
                // On DST fall-back days two wall-clock times can map to
                // overlapping instants; the batch check catches what the DB
                // overlap query cannot see yet.
                boolean clashInBatch = lastAcceptedEnd != null && slotStart.isBefore(lastAcceptedEnd);
                if (clashInBatch || doctorSlotRepository.existsOverlappingSlot(doctorId, slotStart, slotEnd)) {
                    skipped.add(new SlotInterval(slotStart, slotEnd));
                    continue;
                }
                DoctorSlot slot = new DoctorSlot();
                slot.setDoctorId(doctorId);
                slot.setStartAt(slotStart);
                slot.setEndAt(slotEnd);
                slot.setAvailable(true);
                toCreate.add(slot);
                lastAcceptedEnd = slotEnd;
            }
        }

        List<ScheduleSlot> created = doctorSlotRepository.saveAll(toCreate).stream()
                .map(scheduleSlotMapper::toApi)
                .toList();
        return new RecurringScheduleResult(created, skipped);
    }

    /**
     * Deletes an unbooked slot. The pessimistic lock serializes against the
     * booking flow's {@code findAndLockAvailableSlot}, so a slot cannot be
     * deleted and booked at the same time.
     */
    @Transactional
    public void deleteScheduleSlot(UUID doctorId, UUID slotId) {
        DoctorSlot slot = doctorSlotRepository.findAndLockByIdAndDoctorId(slotId, doctorId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            throw new AppointmentStateConflictException("Booked slots cannot be deleted");
        }
        doctorSlotRepository.delete(slot);
    }

    private ZoneId parseZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("timezone is required");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("Unknown timezone: " + timezone, exception);
        }
    }

    private LocalTime parseTime(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must be a valid HH:mm time", exception);
        }
    }

    private String blankToEmpty(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
