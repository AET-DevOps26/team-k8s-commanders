package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.ScheduleSlotCreate;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;

import java.time.OffsetDateTime;
import java.util.List;
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
