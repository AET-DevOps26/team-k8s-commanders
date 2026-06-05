package com.caredesk.patient.config;

import com.caredesk.patient.model.DoctorProfile;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.DoctorProfileRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DefaultPatientDataSeeder implements ApplicationRunner {

    private static final UUID DOCTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CLINIC_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorSlotRepository doctorSlotRepository;

    public DefaultPatientDataSeeder(DoctorProfileRepository doctorProfileRepository,
                                    DoctorSlotRepository doctorSlotRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorSlotRepository = doctorSlotRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!doctorProfileRepository.existsById(DOCTOR_ID)) {
            DoctorProfile doctor = new DoctorProfile();
            doctor.setId(DOCTOR_ID);
            doctor.setName("Doctor");
            doctor.setEmail("doctor@doctor.com");
            doctor.setSpecialization("General Medicine");
            doctor.setLicenseNumber("DE-CARE-1001");
            doctor.setClinicId(CLINIC_ID);
            doctorProfileRepository.save(doctor);
        }
        if (doctorSlotRepository.findByDoctorId(DOCTOR_ID).isEmpty()) {
            defaultSlots().forEach(doctorSlotRepository::save);
        }
    }

    private List<DoctorSlot> defaultSlots() {
        return List.of(
                slot("2026-06-08T09:00:00Z", "2026-06-08T09:30:00Z"),
                slot("2026-06-08T10:00:00Z", "2026-06-08T10:30:00Z"),
                slot("2026-06-09T14:00:00Z", "2026-06-09T14:30:00Z"),
                slot("2026-06-10T11:00:00Z", "2026-06-10T11:30:00Z")
        );
    }

    private DoctorSlot slot(String start, String end) {
        DoctorSlot slot = new DoctorSlot();
        slot.setDoctorId(DOCTOR_ID);
        slot.setStartAt(OffsetDateTime.parse(start).withOffsetSameInstant(ZoneOffset.UTC));
        slot.setEndAt(OffsetDateTime.parse(end).withOffsetSameInstant(ZoneOffset.UTC));
        slot.setAvailable(true);
        return slot;
    }
}
