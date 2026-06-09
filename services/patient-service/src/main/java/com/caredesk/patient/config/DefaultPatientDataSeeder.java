package com.caredesk.patient.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.openapitools.model.AppointmentStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.DoctorProfile;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.model.Patient;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorProfileRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import com.caredesk.patient.repository.PatientRepository;

@Component
@Profile("dev")
public class DefaultPatientDataSeeder implements ApplicationRunner {

    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CLINIC_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final PatientRepository patientRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final AppointmentRepository appointmentRepository;

    public DefaultPatientDataSeeder(PatientRepository patientRepository,
                                    DoctorProfileRepository doctorProfileRepository,
                                    DoctorSlotRepository doctorSlotRepository,
                                    AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorSlotRepository = doctorSlotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedPatient();
        seedDoctor();
        seedSchedule();
    }

    private void seedPatient() {
        Patient patient = patientRepository.findById(PATIENT_ID).orElseGet(Patient::new);
        patient.setId(PATIENT_ID);
        patient.setPhoneNumber("+49 89 123456");
        patient.setDateOfBirth(LocalDate.parse("1990-04-12"));
        patientRepository.save(patient);
    }

    private void seedDoctor() {
        DoctorProfile doctor = doctorProfileRepository.findById(DOCTOR_ID).orElseGet(DoctorProfile::new);
        doctor.setId(DOCTOR_ID);
        doctor.setName("Doctor");
        doctor.setEmail("doctor@doctor.com");
        doctor.setSpecialization("General Medicine");
        doctor.setLicenseNumber("DE-CARE-1001");
        doctor.setClinicId(CLINIC_ID);
        doctorProfileRepository.save(doctor);
    }

    private void seedSchedule() {
        defaultAppointments().forEach(this::seedAppointment);
        defaultOpenSlots().forEach(slot -> seedSlot(slot, true));
    }

    private List<DefaultAppointment> defaultAppointments() {
        return List.of(
                new DefaultAppointment(
                        dayAt(-14, 10),
                        30,
                        "Annual check-up",
                        AppointmentStatus.COMPLETED),
                new DefaultAppointment(
                        dayAt(2, 9),
                        30,
                        "Blood pressure follow-up",
                        AppointmentStatus.SCHEDULED)
        );
    }

    private List<DefaultSlot> defaultOpenSlots() {
        return List.of(
                new DefaultSlot(dayAt(2, 11), 30),
                new DefaultSlot(dayAt(4, 15), 30),
                new DefaultSlot(dayAt(6, 9), 45),
                new DefaultSlot(dayAt(7, 13), 30)
        );
    }

    private void seedAppointment(DefaultAppointment defaultAppointment) {
        OffsetDateTime startAt = defaultAppointment.startAt();
        seedSlot(new DefaultSlot(startAt, defaultAppointment.duration()), false);
        Appointment appointment = appointmentRepository
                .findFirstByPatientIdAndDoctorIdAndReason(PATIENT_ID, DOCTOR_ID, defaultAppointment.reason())
                .orElseGet(Appointment::new);
        appointment.setPatientId(PATIENT_ID);
        appointment.setDoctorId(DOCTOR_ID);
        appointment.setDateTime(startAt);
        appointment.setDuration(defaultAppointment.duration());
        appointment.setReason(defaultAppointment.reason());
        appointment.setStatus(defaultAppointment.status());
        appointmentRepository.save(appointment);
    }

    private void seedSlot(DefaultSlot defaultSlot, boolean available) {
        OffsetDateTime startAt = defaultSlot.startAt();
        OffsetDateTime endAt = startAt.plusMinutes(defaultSlot.duration());
        DoctorSlot slot = doctorSlotRepository
                .findSlotByTime(DOCTOR_ID, startAt, endAt)
                .orElseGet(DoctorSlot::new);
        slot.setDoctorId(DOCTOR_ID);
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        slot.setAvailable(available);
        doctorSlotRepository.save(slot);
    }

    private OffsetDateTime dayAt(int daysFromToday, int hour) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(daysFromToday)
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private record DefaultSlot(OffsetDateTime startAt, int duration) {
    }

    private record DefaultAppointment(OffsetDateTime startAt,
                                      int duration,
                                      String reason,
                                      AppointmentStatus status) {
    }
}
