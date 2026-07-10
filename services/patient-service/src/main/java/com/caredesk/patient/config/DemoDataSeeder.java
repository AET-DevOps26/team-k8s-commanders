package com.caredesk.patient.config;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.openapitools.model.AppointmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Seeds a coherent demo dataset for presentations so every dashboard is
 * populated straight after startup (see issue #168).
 *
 * <p>This is the single source of demo data for patient-service — it owns only
 * the scheduling slice (appointments and doctor slots). The patient and doctor
 * <em>identities</em> live in auth-service and are seeded there by its
 * {@code DemoDataSeeder} (patient, doctor and Anna Müller); this seeder just
 * references their ids.
 *
 * <p>Enabled only in the {@code dev} profile — the single switch for all demo
 * seeding; it never runs in production. Idempotent: appointments are keyed on a
 * fixed UUID and upserted, and {@code now} is truncated to the top of the hour
 * so slot times (keyed by doctor + start/end) match on restart rather than
 * accumulating.
 *
 * <p><strong>Cross-service IDs.</strong> The {@code DEMO_*} UUIDs below are the
 * canonical demo identifiers and are shared with the demo seeders in
 * auth-service (patient/doctor identities), notes-service (clinical notes) and
 * notification-service (notification records). Keep them in sync — appointments,
 * notes and notifications all reference the same appointment/patient/doctor ids.
 */
@Component
@Profile("dev")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Canonical doctor (doctor@doctor.com), seeded by auth-service's DemoDataSeeder. */
    static final UUID DOCTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    /** Extra demo doctors (ids match auth-service's DemoDataSeeder) — give the booking
     *  flow open slots under more than one specialization (Cardiology, Pediatrics). */
    static final UUID DOCTOR_CARDIOLOGY_ID = UUID.fromString("22222222-2222-2222-2222-000000000002");
    static final UUID DOCTOR_PEDIATRICS_ID = UUID.fromString("22222222-2222-2222-2222-000000000003");
    /** Second General Medicine doctor — gives that specialization more than one doctor. */
    static final UUID DOCTOR_GENERAL_2_ID = UUID.fromString("22222222-2222-2222-2222-000000000004");
    /** Canonical patient (patient@patient.com), seeded by auth-service's DemoDataSeeder. */
    static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** Second demo patient — "Anna Müller", the pitch's AI-assistant example (seeded in auth-service). */
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

    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository doctorSlotRepository;

    /**
     * @param appointmentRepository appointment rows
     * @param doctorSlotRepository  bookable slot rows
     */
    public DemoDataSeeder(AppointmentRepository appointmentRepository,
                          DoctorSlotRepository doctorSlotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorSlotRepository = doctorSlotRepository;
    }

    /**
     * Seeds a spread of appointments across every status (including one due
     * within 24h) for the canonical patient and Anna, plus a few open future
     * slots so the booking flow has availability to show.
     *
     * @param args ignored
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Truncated to the top of the hour so relative demo times are clean
        // (e.g. 10:00, not 10:47) and stable across same-day restarts.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC)
                .withMinute(0).withSecond(0).withNano(0);

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

        // Open future slots so the booking flow has availability to show — a few
        // per doctor so every seeded specialization is bookable.
        upsertSlot(DOCTOR_ID, now.plusDays(2).withHour(10), 30);
        upsertSlot(DOCTOR_ID, now.plusDays(2).withHour(14), 30);
        upsertSlot(DOCTOR_ID, now.plusDays(4).withHour(9), 45);
        upsertSlot(DOCTOR_CARDIOLOGY_ID, now.plusDays(2).withHour(11), 30);
        upsertSlot(DOCTOR_CARDIOLOGY_ID, now.plusDays(3).withHour(15), 30);
        upsertSlot(DOCTOR_PEDIATRICS_ID, now.plusDays(2).withHour(9), 30);
        upsertSlot(DOCTOR_PEDIATRICS_ID, now.plusDays(5).withHour(13), 30);
        upsertSlot(DOCTOR_GENERAL_2_ID, now.plusDays(3).withHour(10), 30);
        upsertSlot(DOCTOR_GENERAL_2_ID, now.plusDays(4).withHour(14), 30);

        log.info("Demo dataset seeded (patient-service): 8 appointments, 9 open slots across 4 doctors");
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

    private void upsertSlot(UUID doctorId, OffsetDateTime startAt, int durationMinutes) {
        OffsetDateTime endAt = startAt.plusMinutes(durationMinutes);
        DoctorSlot slot = doctorSlotRepository.findSlotByTime(doctorId, startAt, endAt).orElseGet(DoctorSlot::new);
        slot.setDoctorId(doctorId);
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        slot.setAvailable(true);
        doctorSlotRepository.save(slot);
    }
}
