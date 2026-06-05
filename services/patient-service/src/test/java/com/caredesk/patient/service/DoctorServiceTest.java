package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.junit.jupiter.api.Test;
import org.openapitools.model.Schedule;
import org.openapitools.model.UserProfile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoctorService}. The repository is mocked so the
 * tests do not need a database.
 */
class DoctorServiceTest {

    private final DoctorSlotRepository repository = mock(DoctorSlotRepository.class);
    private final ScheduleSlotMapper mapper = new ScheduleSlotMapper();
    private final DoctorService service = new DoctorService(repository, mapper);

    @Test
    void getProfile_returnsIdOnly() {
        UUID doctorId = UUID.randomUUID();

        UserProfile profile = service.getProfile(doctorId);

        assertThat(profile.getId()).isEqualTo(doctorId);
        // Identity fields live in auth-service and stay null in the patient-service response.
        assertThat(profile.getName()).isNull();
        assertThat(profile.getEmail()).isNull();
        assertThat(profile.getRole()).isNull();
        assertThat(profile.getSpecialization()).isNull();
        assertThat(profile.getLicenseNumber()).isNull();
    }

    @Test
    void getSchedule_returnsMappedSlots() {
        UUID doctorId = UUID.randomUUID();
        DoctorSlot a = slot(doctorId, "2026-06-10T09:00:00Z", "2026-06-10T09:30:00Z", true);
        DoctorSlot b = slot(doctorId, "2026-06-10T09:30:00Z", "2026-06-10T10:00:00Z", false);
        when(repository.findByDoctorId(doctorId)).thenReturn(List.of(a, b));

        Schedule schedule = service.getSchedule(doctorId);

        assertThat(schedule.getDoctorId()).isEqualTo(doctorId);
        assertThat(schedule.getSlots()).hasSize(2);
        assertThat(schedule.getSlots().get(0).getStartAt())
                .isEqualTo(OffsetDateTime.parse("2026-06-10T09:00:00Z"));
        assertThat(schedule.getSlots().get(0).getAvailable()).isTrue();
        assertThat(schedule.getSlots().get(1).getAvailable()).isFalse();
    }

    @Test
    void getSchedule_returnsEmptySchedule_whenNoSlots() {
        UUID doctorId = UUID.randomUUID();
        when(repository.findByDoctorId(doctorId)).thenReturn(List.of());

        Schedule schedule = service.getSchedule(doctorId);

        assertThat(schedule.getDoctorId()).isEqualTo(doctorId);
        assertThat(schedule.getSlots()).isEmpty();
    }

    private static DoctorSlot slot(UUID doctorId, String startAt, String endAt, boolean available) {
        DoctorSlot s = new DoctorSlot();
        s.setId(UUID.randomUUID());
        s.setDoctorId(doctorId);
        s.setStartAt(OffsetDateTime.parse(startAt));
        s.setEndAt(OffsetDateTime.parse(endAt));
        s.setAvailable(available);
        return s;
    }
}
