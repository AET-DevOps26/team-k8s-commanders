package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.RecurringScheduleCreate;
import org.openapitools.model.RecurringScheduleResult;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.openapitools.model.Weekday;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorServiceTest {

    private final DoctorSlotRepository doctorSlotRepository = mock(DoctorSlotRepository.class);
    private final ScheduleSlotMapper scheduleSlotMapper = new ScheduleSlotMapper();
    private final AuthServiceClient authServiceClient = mock(AuthServiceClient.class);
    private final DoctorService service = new DoctorService(
            doctorSlotRepository, scheduleSlotMapper, authServiceClient);

    @Test
    void listDoctors_readsThroughToAuthService() {
        PaginatedUserProfileResponse expected = response(doctor(UUID.randomUUID(), "Doctor"));
        when(authServiceClient.searchDoctors(eq("general"), eq("medicine"), eq(0), eq(20)))
                .thenReturn(expected);

        PaginatedUserProfileResponse response = service.listDoctors(" general ", " medicine ", 0, 20);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void listDoctors_usesEmptySearchTerms_whenFiltersAreBlank() {
        PaginatedUserProfileResponse expected = response(doctor(UUID.randomUUID(), "Doctor"));
        when(authServiceClient.searchDoctors(eq(""), eq(""), eq(0), eq(12))).thenReturn(expected);

        PaginatedUserProfileResponse response = service.listDoctors(null, " ", 0, 12);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void listSpecializations_readsThroughToAuthService() {
        when(authServiceClient.getSpecializations()).thenReturn(List.of("Cardiology", "General Medicine"));

        assertThat(service.listSpecializations()).containsExactly("Cardiology", "General Medicine");
    }

    @Test
    void getProfile_returnsAuthServiceProfile_whenFound() {
        UUID doctorId = UUID.randomUUID();
        UserProfile authProfile = doctor(doctorId, "Dr Who");
        authProfile.setPhoneNumber("+1-555-0100");
        authProfile.setDateOfBirth(java.time.LocalDate.of(1970, 1, 1));
        when(authServiceClient.getUserById(doctorId)).thenReturn(authProfile);

        UserProfile profile = service.getProfile(doctorId);

        assertThat(profile.getId()).isEqualTo(doctorId);
        assertThat(profile.getName()).isEqualTo("Dr Who");
        assertThat(profile.getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(profile.getPhoneNumber()).isNull();
        assertThat(profile.getDateOfBirth()).isNull();
    }

    @Test
    void getProfile_fallsBackToIdOnly_whenAuthServiceMisses() {
        UUID doctorId = UUID.randomUUID();
        when(authServiceClient.getUserById(doctorId)).thenReturn(null);

        UserProfile profile = service.getProfile(doctorId);

        assertThat(profile.getId()).isEqualTo(doctorId);
        assertThat(profile.getName()).isNull();
        assertThat(profile.getEmail()).isNull();
    }

    @Test
    void getSchedule_returnsOnlyAvailableSlotsSortedByStart() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime early = OffsetDateTime.parse("2026-06-08T09:00:00Z");
        OffsetDateTime late = OffsetDateTime.parse("2026-06-08T10:00:00Z");
        when(doctorSlotRepository.findByDoctorId(doctorId)).thenReturn(List.of(
                slot(doctorId, late, true),
                slot(doctorId, early, true),
                slot(doctorId, early.plusHours(2), false)
        ));

        Schedule schedule = service.getSchedule(doctorId);

        assertThat(schedule.getSlots()).hasSize(2);
        assertThat(schedule.getSlots().get(0).getStartAt()).isEqualTo(early);
        assertThat(schedule.getSlots().get(1).getStartAt()).isEqualTo(late);
    }

    @Test
    void getSchedule_returnsEmptySchedule_whenNoSlots() {
        UUID doctorId = UUID.randomUUID();
        when(doctorSlotRepository.findByDoctorId(doctorId)).thenReturn(List.of());

        Schedule schedule = service.getSchedule(doctorId);

        assertThat(schedule.getDoctorId()).isEqualTo(doctorId);
        assertThat(schedule.getSlots()).isEmpty();
    }

    @Test
    void verifyDoctorExists_passes_forAuthDoctor() {
        UUID doctorId = UUID.randomUUID();
        when(authServiceClient.getUserById(doctorId)).thenReturn(doctor(doctorId, "Dr. Admin Created"));

        service.verifyDoctorExists(doctorId);
    }

    @Test
    void verifyDoctorExists_rejectsUnknownUser() {
        UUID doctorId = UUID.randomUUID();
        when(authServiceClient.getUserById(doctorId)).thenReturn(null);

        assertThatThrownBy(() -> service.verifyDoctorExists(doctorId))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void verifyDoctorExists_rejectsNonDoctorRole() {
        UUID doctorId = UUID.randomUUID();
        UserProfile patient = new UserProfile(doctorId, "Pat", "pat@x.com", UserRole.PATIENT);
        when(authServiceClient.getUserById(doctorId)).thenReturn(patient);

        assertThatThrownBy(() -> service.verifyDoctorExists(doctorId))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void createScheduleSlot_persistsAvailableSlot() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2035-06-08T09:00:00Z");
        OffsetDateTime endAt = startAt.plusMinutes(45);
        ScheduleSlotCreate request = new ScheduleSlotCreate(startAt, endAt);
        when(doctorSlotRepository.existsOverlappingSlot(doctorId, startAt, endAt)).thenReturn(false);
        when(doctorSlotRepository.save(any(DoctorSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleSlot slot = service.createScheduleSlot(doctorId, request);

        assertThat(slot.getStartAt()).isEqualTo(startAt);
        assertThat(slot.getEndAt()).isEqualTo(endAt);
        assertThat(slot.getAvailable()).isTrue();
        verify(doctorSlotRepository).lockForSlotWrite(doctorId);
        verify(doctorSlotRepository).save(any(DoctorSlot.class));
    }

    @Test
    void createScheduleSlot_rejectsOverlappingSlot() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2035-06-08T09:00:00Z");
        OffsetDateTime endAt = startAt.plusMinutes(30);
        ScheduleSlotCreate request = new ScheduleSlotCreate(startAt, endAt);
        when(doctorSlotRepository.existsOverlappingSlot(doctorId, startAt, endAt)).thenReturn(true);

        assertThatThrownBy(() -> service.createScheduleSlot(doctorId, request))
                .isInstanceOf(AppointmentStateConflictException.class)
                .hasMessageContaining("overlaps");
        verify(doctorSlotRepository, never()).save(any());
    }

    @Test
    void createScheduleSlot_rejectsPastSlot() {
        UUID doctorId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2020-06-08T09:00:00Z");
        ScheduleSlotCreate request = new ScheduleSlotCreate(startAt, startAt.plusMinutes(30));

        assertThatThrownBy(() -> service.createScheduleSlot(doctorId, request))
                .isInstanceOf(AppointmentStateConflictException.class)
                .hasMessageContaining("Past schedule slots");
        verify(doctorSlotRepository, never()).save(any());
    }

    @Test
    void createRecurringScheduleSlots_expandsSelectedWeekdays() {
        UUID doctorId = UUID.randomUUID();
        // 2035-01-01 is a Monday; Mon+Wed over two weeks -> Jan 1, 3, 8, 10.
        RecurringScheduleCreate request = recurring(
                Set.of(Weekday.MONDAY, Weekday.WEDNESDAY), "09:00", "12:00",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30,
                LocalDate.of(2035, 1, 1), LocalDate.of(2035, 1, 14));
        stubSaveAll();

        RecurringScheduleResult result = service.createRecurringScheduleSlots(doctorId, request);

        assertThat(result.getCreated()).hasSize(24);
        assertThat(result.getSkipped()).isEmpty();
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        ScheduleSlot first = result.getCreated().get(0);
        assertThat(first.getStartAt().atZoneSameInstant(berlin).toLocalDateTime())
                .isEqualTo(LocalDate.of(2035, 1, 1).atTime(9, 0));
        ScheduleSlot last = result.getCreated().get(23);
        assertThat(last.getEndAt().atZoneSameInstant(berlin).toLocalDateTime())
                .isEqualTo(LocalDate.of(2035, 1, 10).atTime(12, 0));
        verify(doctorSlotRepository).lockForSlotWrite(doctorId);
    }

    @Test
    void createRecurringScheduleSlots_keepsWallClockAcrossDstSpringForward() {
        UUID doctorId = UUID.randomUUID();
        // Europe/Berlin switches to DST on Sunday 2035-03-25.
        RecurringScheduleCreate request = recurring(
                Set.of(Weekday.SUNDAY), "09:00", "10:00",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_60,
                LocalDate.of(2035, 3, 18), LocalDate.of(2035, 3, 25));
        stubSaveAll();

        RecurringScheduleResult result = service.createRecurringScheduleSlots(doctorId, request);

        assertThat(result.getCreated()).hasSize(2);
        assertThat(result.getCreated().get(0).getStartAt().getOffset()).isEqualTo(ZoneOffset.ofHours(1));
        assertThat(result.getCreated().get(1).getStartAt().getOffset()).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(result.getCreated().get(0).getStartAt().toLocalTime()).isEqualTo(java.time.LocalTime.of(9, 0));
        assertThat(result.getCreated().get(1).getStartAt().toLocalTime()).isEqualTo(java.time.LocalTime.of(9, 0));
    }

    @Test
    void createRecurringScheduleSlots_skipsOverlaps_andReportsThem() {
        UUID doctorId = UUID.randomUUID();
        RecurringScheduleCreate request = recurring(
                Set.of(Weekday.MONDAY), "09:00", "10:00",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30,
                LocalDate.of(2035, 1, 1), LocalDate.of(2035, 1, 1));
        OffsetDateTime clashStart = LocalDate.of(2035, 1, 1).atTime(9, 0)
                .atZone(ZoneId.of("Europe/Berlin")).toOffsetDateTime();
        when(doctorSlotRepository.existsOverlappingSlot(eq(doctorId), eq(clashStart), any()))
                .thenReturn(true);
        stubSaveAll();

        RecurringScheduleResult result = service.createRecurringScheduleSlots(doctorId, request);

        assertThat(result.getCreated()).hasSize(1);
        assertThat(result.getCreated().get(0).getStartAt()).isEqualTo(clashStart.plusMinutes(30));
        assertThat(result.getSkipped()).hasSize(1);
        assertThat(result.getSkipped().get(0).getStartAt()).isEqualTo(clashStart);
    }

    @Test
    void createRecurringScheduleSlots_dropsPartialRemainder() {
        UUID doctorId = UUID.randomUUID();
        RecurringScheduleCreate request = recurring(
                Set.of(Weekday.MONDAY), "09:00", "10:10",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30,
                LocalDate.of(2035, 1, 1), LocalDate.of(2035, 1, 1));
        stubSaveAll();

        RecurringScheduleResult result = service.createRecurringScheduleSlots(doctorId, request);

        assertThat(result.getCreated()).hasSize(2);
    }

    @Test
    void createRecurringScheduleSlots_dropsPastOccurrencesSilently() {
        UUID doctorId = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now(ZoneId.of("Europe/Berlin")).minusDays(1);
        RecurringScheduleCreate request = recurring(
                Set.of(Weekday.fromValue(yesterday.getDayOfWeek().name())), "09:00", "10:00",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30,
                yesterday, yesterday);
        stubSaveAll();

        RecurringScheduleResult result = service.createRecurringScheduleSlots(doctorId, request);

        assertThat(result.getCreated()).isEmpty();
        assertThat(result.getSkipped()).isEmpty();
    }

    @Test
    void createRecurringScheduleSlots_terminatesNearMidnight() {
        UUID doctorId = UUID.randomUUID();
        RecurringScheduleCreate request = recurring(
                Set.of(Weekday.MONDAY), "23:00", "23:59",
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30,
                LocalDate.of(2035, 1, 1), LocalDate.of(2035, 1, 1));
        stubSaveAll();

        RecurringScheduleResult result = service.createRecurringScheduleSlots(doctorId, request);

        assertThat(result.getCreated()).hasSize(1);
    }

    @Test
    void createRecurringScheduleSlots_rejectsInvalidRequests() {
        UUID doctorId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2035, 1, 1);
        RecurringScheduleCreate.SlotDurationMinutesEnum thirty =
                RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_30;

        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(), "09:00", "12:00", thirty, start, start)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("weekday");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(Weekday.MONDAY), "12:00", "09:00", thirty, start, start)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("endTime");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(Weekday.MONDAY), "09:00", "09:15", thirty, start, start)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("shorter than one slot");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(Weekday.MONDAY), "23:00", "23:59",
                        RecurringScheduleCreate.SlotDurationMinutesEnum.NUMBER_60, start, start)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("shorter than one slot");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(Weekday.MONDAY), "09:00", "12:00", thirty, start, start.plusDays(85))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("12 weeks");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(Weekday.MONDAY), "09:00", "12:00", thirty, start, start.minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("endDate");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                new RecurringScheduleCreate(Set.of(Weekday.MONDAY), "09:00", "12:00", thirty,
                        start, start, "Mars/Olympus")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timezone");
        assertThatThrownBy(() -> service.createRecurringScheduleSlots(doctorId,
                recurring(Set.of(Weekday.MONDAY), "9 o'clock", "12:00", thirty, start, start)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("startTime");
        verify(doctorSlotRepository, never()).saveAll(any());
    }

    @Test
    void deleteScheduleSlot_deletesAvailableSlot() {
        UUID doctorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        DoctorSlot slot = slot(doctorId, OffsetDateTime.parse("2035-06-08T09:00:00Z"), true);
        when(doctorSlotRepository.findAndLockByIdAndDoctorId(slotId, doctorId)).thenReturn(Optional.of(slot));

        service.deleteScheduleSlot(doctorId, slotId);

        verify(doctorSlotRepository).delete(slot);
    }

    @Test
    void deleteScheduleSlot_rejectsBookedSlot() {
        UUID doctorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        DoctorSlot slot = slot(doctorId, OffsetDateTime.parse("2035-06-08T09:00:00Z"), false);
        when(doctorSlotRepository.findAndLockByIdAndDoctorId(slotId, doctorId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.deleteScheduleSlot(doctorId, slotId))
                .isInstanceOf(AppointmentStateConflictException.class)
                .hasMessageContaining("Booked");
        verify(doctorSlotRepository, never()).delete(any(DoctorSlot.class));
    }

    @Test
    void deleteScheduleSlot_throwsNotFound_whenMissingOrForeign() {
        UUID doctorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(doctorSlotRepository.findAndLockByIdAndDoctorId(slotId, doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteScheduleSlot(doctorId, slotId))
                .isInstanceOf(SlotNotFoundException.class);
    }

    private void stubSaveAll() {
        when(doctorSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static RecurringScheduleCreate recurring(Set<Weekday> weekdays,
                                                     String startTime,
                                                     String endTime,
                                                     RecurringScheduleCreate.SlotDurationMinutesEnum duration,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        return new RecurringScheduleCreate(weekdays, startTime, endTime, duration,
                startDate, endDate, "Europe/Berlin");
    }

    private static UserProfile doctor(UUID id, String name) {
        UserProfile profile = new UserProfile(id, name, "doctor@clinic.com", UserRole.DOCTOR);
        profile.setSpecialization("General Medicine");
        return profile;
    }

    private static PaginatedUserProfileResponse response(UserProfile... doctors) {
        return new PaginatedUserProfileResponse(
                List.of(doctors), new PageMeta(0, 20, (long) doctors.length, 1));
    }

    private static DoctorSlot slot(UUID doctorId, OffsetDateTime startAt, boolean available) {
        DoctorSlot slot = new DoctorSlot();
        slot.setDoctorId(doctorId);
        slot.setStartAt(startAt);
        slot.setEndAt(startAt.plusMinutes(30));
        slot.setAvailable(available);
        return slot;
    }
}
