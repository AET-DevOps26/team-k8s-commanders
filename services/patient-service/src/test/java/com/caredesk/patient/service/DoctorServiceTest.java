package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorProfile;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorProfileRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.Schedule;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorServiceTest {

    private final DoctorProfileRepository doctorProfileRepository = mock(DoctorProfileRepository.class);
    private final DoctorSlotRepository doctorSlotRepository = mock(DoctorSlotRepository.class);
    private final DoctorService service = new DoctorService(doctorProfileRepository, doctorSlotRepository);

    @Test
    void listDoctors_mapsSearchResults() {
        DoctorProfile doctor = doctor();
        when(doctorProfileRepository.search(eq("general"), eq("medicine"), any()))
                .thenReturn(new PageImpl<>(List.of(doctor), PageRequest.of(0, 20), 1));

        PaginatedUserProfileResponse response = service.listDoctors(" general ", " medicine ", 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(doctor.getId());
        assertThat(response.getContent().getFirst().getSpecialization()).isEqualTo("General Medicine");
        assertThat(response.getPage().getTotalElements()).isEqualTo(1);
    }

    @Test
    void listDoctors_usesEmptySearchTerms_whenFiltersAreBlank() {
        DoctorProfile doctor = doctor();
        when(doctorProfileRepository.search(eq(""), eq(""), any()))
                .thenReturn(new PageImpl<>(List.of(doctor), PageRequest.of(0, 12), 1));

        PaginatedUserProfileResponse response = service.listDoctors(null, " ", 0, 12);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getName()).isEqualTo("Doctor");
    }

    @Test
    void getSchedule_returnsOnlyAvailableSlotsSortedByStart() {
        DoctorProfile doctor = doctor();
        OffsetDateTime early = OffsetDateTime.parse("2026-06-08T09:00:00Z");
        OffsetDateTime late = OffsetDateTime.parse("2026-06-08T10:00:00Z");
        when(doctorProfileRepository.existsById(doctor.getId())).thenReturn(true);
        when(doctorSlotRepository.findByDoctorId(doctor.getId())).thenReturn(List.of(
                slot(doctor.getId(), late, true),
                slot(doctor.getId(), early, true),
                slot(doctor.getId(), early.plusHours(2), false)
        ));

        Schedule schedule = service.getSchedule(doctor.getId());

        assertThat(schedule.getSlots()).hasSize(2);
        assertThat(schedule.getSlots().get(0).getStartAt()).isEqualTo(early);
        assertThat(schedule.getSlots().get(1).getStartAt()).isEqualTo(late);
    }

    private static DoctorProfile doctor() {
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(UUID.randomUUID());
        doctor.setName("Doctor");
        doctor.setEmail("doctor@doctor.com");
        doctor.setSpecialization("General Medicine");
        doctor.setLicenseNumber("DE-CARE-1001");
        doctor.setClinicId(UUID.randomUUID());
        return doctor;
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
