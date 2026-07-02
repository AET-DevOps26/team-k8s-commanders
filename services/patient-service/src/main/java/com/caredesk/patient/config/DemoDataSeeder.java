package com.caredesk.patient.config;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.model.Patient;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import com.caredesk.patient.repository.PatientRepository;
import org.openapitools.model.AppointmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Seeds a coherent demo dataset for presentations so every dashboard is
 * populated straight after startup (see issue #168).
 *
 * <p>Enabled only in the {@code dev} profile and when
 * {@code caredesk.seed.demo=true} (env {@code CAREDESK_SEED_DEMO}); it never
 * runs in production. Idempotent: every row is keyed on a fixed UUID and
 * upserted, so restarts don't duplicate or reset data.
 *
 * <p><strong>Cross-service IDs.</strong> The {@code DEMO_*} UUIDs below are the
 * canonical demo identifiers and are replicated verbatim by the demo seeders in
 * auth-service (patient/doctor identities), notes-service (clinical notes) and
 * notification-service (notification records). Keep them in sync — appointments,
 * notes and notifications all reference the same appointment/patient/doctor ids.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "caredesk.seed", name = "demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Canonical doctor (doctor@doctor.com), already seeded by DefaultUserSeeder / DefaultPatientDataSeeder. */
    static final UUID DOCTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    /** Canonical patient (patient@patient.com). */
    static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** Second demo patient — "Anna Müller", the pitch's AI-assistant example. */
    static final UUID ANNA_ID = UUID.fromString("d0000000-0000-0000-0000-0000000000a1");

    // Fixed appointment ids so notes-service and notification-service can link to them.
    static final UUID APPT_HTN_PAST = UUID.fromString("d0000000-0000-0000-0000-000000000101");
    static final UUID APPT_BP_FOLLOWUP = UUID.fromString("d0000000-0000-0000-0000-000000000102");
    static final UUID APPT_UPCOMING_24H = UUID.fromString("d0000000-0000-0000-0000-000000000103");
    static final UUID APPT_RESCHEDULED = UUID.fromString("d0000000-0000-0000-0000-000000000104");
    static final UUID APPT_CANCELLED = UUID.fromString("d0000000-0000-0000-0000-000000000105");
    static final UUID APPT_ANNA_DM_1 = UUID.fromString("d0000000-0000-0000-0000-000000000106");
    static final UUID APPT_ANNA_DM_2 = UUID.fromString("d0000000-0000-0000-0000-000000000107");
    static final UUID APPT_ANNA_UPCOMING = UUID.fromString("d0000000-0000-0000-0000-000000000108");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository doctorSlotRepository;

    /**
     * @param patientRepository     patient rows
     * @param appointmentRepository appointment rows
     * @param doctorSlotRepository  bookable slot rows
     */
    public DemoDataSeeder(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          DoctorSlotRepository doctorSlotRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorSlotRepository = doctorSlotRepository;
    }

    /**
     * Seeds the demo patient, a spread of appointments across every status
     * (including one due within 24h) and a few open future slots.
     *
     * @param args ignored
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // "Anna Müller" — clinical slice only; her identity/name live in auth-service.
        Patient anna = patientRepository.findById(ANNA_ID).orElseGet(Patient::new);
        anna.setId(ANNA_ID);
        anna.setPhoneNumber("+49 89 445566");
        anna.setDateOfBirth(LocalDate.parse("1975-06-30"));
        patientRepository.save(anna);

        // Canonical patient — a full history plus one appointment inside the 24h reminder window.
        upsertAppointment(APPT_HTN_PAST, PATIENT_ID, now.minusDays(30), 30,
                AppointmentStatus.COMPLETED, "Hypertension review");
        upsertAppointment(APPT_BP_FOLLOWUP, PATIENT_ID, now.minusDays(14), 30,
                AppointmentStatus.COMPLETED, "Blood pressure follow-up");
        upsertAppointment(APPT_UPCOMING_24H, PATIENT_ID, now.plusHours(12), 30,
                AppointmentStatus.SCHEDULED, "Annual check-up");
        upsertAppointment(APPT_RESCHEDULED, PATIENT_ID, now.plusDays(5), 45,
                AppointmentStatus.RESCHEDULED, "Consultation");
        upsertAppointment(APPT_CANCELLED, PATIENT_ID, now.plusDays(3), 30,
                AppointmentStatus.CANCELLED, "Consultation");

        // Anna — Type 2 diabetes history for the AI-assistant demo query.
        upsertAppointment(APPT_ANNA_DM_1, ANNA_ID, now.minusDays(60), 30,
                AppointmentStatus.COMPLETED, "Type 2 diabetes review");
        upsertAppointment(APPT_ANNA_DM_2, ANNA_ID, now.minusDays(20), 30,
                AppointmentStatus.COMPLETED, "Diabetes follow-up");
        upsertAppointment(APPT_ANNA_UPCOMING, ANNA_ID, now.plusDays(7), 30,
                AppointmentStatus.SCHEDULED, "Diabetes review");

        // A few open future slots so the booking flow has availability to show.
        upsertSlot(now.plusDays(2).withHour(10), 30);
        upsertSlot(now.plusDays(2).withHour(14), 30);
        upsertSlot(now.plusDays(4).withHour(9), 45);

        log.info("Demo dataset seeded (patient-service): 2 patients, 8 appointments, 3 open slots");
    }

    private void upsertAppointment(UUID id, UUID patientId, OffsetDateTime when, int duration,
                                   AppointmentStatus status, String reason) {
        Appointment appointment = appointmentRepository.findById(id).orElseGet(Appointment::new);
        appointment.setId(id);
        appointment.setPatientId(patientId);
        appointment.setDoctorId(DOCTOR_ID);
        appointment.setDateTime(when);
        appointment.setDuration(duration);
        appointment.setStatus(status);
        appointment.setReason(reason);
        appointmentRepository.save(appointment);
    }

    private void upsertSlot(OffsetDateTime startAt, int durationMinutes) {
        OffsetDateTime endAt = startAt.plusMinutes(durationMinutes);
        DoctorSlot slot = doctorSlotRepository.findSlotByTime(DOCTOR_ID, startAt, endAt).orElseGet(DoctorSlot::new);
        slot.setDoctorId(DOCTOR_ID);
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        slot.setAvailable(true);
        doctorSlotRepository.save(slot);
    }
}
