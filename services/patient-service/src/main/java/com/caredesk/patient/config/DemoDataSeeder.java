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
import java.util.Map;
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
    /** Additional demo patients (ids match auth-service's DemoDataSeeder) — give the
     *  roster more entries, each with a small appointment history and notes. */
    static final UUID MAX_ID = UUID.fromString("d0000000-0000-0000-0000-0000000000a2");
    static final UUID LENA_ID = UUID.fromString("d0000000-0000-0000-0000-0000000000a3");

    // Fixed appointment ids so notes-service and notification-service can link to them.
    static final UUID APPT_HTN_PAST = UUID.fromString("d0000000-0000-0000-0000-000000000101");
    static final UUID APPT_BP_FOLLOWUP = UUID.fromString("d0000000-0000-0000-0000-000000000102");
    static final UUID APPT_UPCOMING_24H = UUID.fromString("d0000000-0000-0000-0000-000000000103");
    static final UUID APPT_RESCHEDULED = UUID.fromString("d0000000-0000-0000-0000-000000000104");
    static final UUID APPT_CANCELLED = UUID.fromString("d0000000-0000-0000-0000-000000000105");
    static final UUID APPT_ANNA_DM_1 = UUID.fromString("d0000000-0000-0000-0000-000000000106");
    static final UUID APPT_ANNA_DM_2 = UUID.fromString("d0000000-0000-0000-0000-000000000107");
    static final UUID APPT_ANNA_UPCOMING = UUID.fromString("d0000000-0000-0000-0000-000000000108");
    // Max Schmidt — two completed visits (with notes) plus a rescheduled and a cancelled.
    static final UUID APPT_MAX_CHECKUP = UUID.fromString("d0000000-0000-0000-0000-000000000109");
    static final UUID APPT_MAX_BACK_PAIN = UUID.fromString("d0000000-0000-0000-0000-00000000010a");
    static final UUID APPT_MAX_RESCHEDULED = UUID.fromString("d0000000-0000-0000-0000-00000000010b");
    static final UUID APPT_MAX_CANCELLED = UUID.fromString("d0000000-0000-0000-0000-00000000010c");
    // Lena Fischer — two completed visits (with notes) plus a rescheduled and a cancelled.
    static final UUID APPT_LENA_ASTHMA = UUID.fromString("d0000000-0000-0000-0000-00000000010d");
    static final UUID APPT_LENA_ASTHMA_FOLLOWUP = UUID.fromString("d0000000-0000-0000-0000-00000000010e");
    static final UUID APPT_LENA_RESCHEDULED = UUID.fromString("d0000000-0000-0000-0000-00000000010f");
    static final UUID APPT_LENA_CANCELLED = UUID.fromString("d0000000-0000-0000-0000-000000000110");

    /**
     * Demo patient contact emails, mirroring auth-service's DemoDataSeeder. The
     * notification service delivers reminders and reschedule/cancel mails to the
     * address stored on the appointment; the reminder feed even skips rows with
     * no email. Normal booking resolves this from auth-service, but the seeder
     * uses the known constants rather than a cross-service call at startup.
     */
    private static final Map<UUID, String> PATIENT_EMAILS = Map.of(
            PATIENT_ID, "patient@patient.com",
            ANNA_ID, "anna.mueller@caredesk.dev",
            MAX_ID, "max.schmidt@caredesk.dev",
            LENA_ID, "lena.fischer@caredesk.dev");

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

        // Max Schmidt — two completed visits (each gets a clinical note) plus a
        // rescheduled and a cancelled appointment for status variety.
        upsertAppointment(APPT_MAX_CHECKUP, MAX_ID, now.minusDays(45), 30,
                AppointmentStatus.COMPLETED, "General check-up");
        upsertAppointment(APPT_MAX_BACK_PAIN, MAX_ID, now.minusDays(10), 30,
                AppointmentStatus.COMPLETED, "Back pain assessment");
        upsertAppointment(APPT_MAX_RESCHEDULED, MAX_ID, now.plusDays(6), 30,
                AppointmentStatus.RESCHEDULED, "Physiotherapy review");
        upsertAppointment(APPT_MAX_CANCELLED, MAX_ID, now.plusDays(4), 30,
                AppointmentStatus.CANCELLED, "Consultation");

        // Lena Fischer — two completed visits (each gets a clinical note) plus a
        // rescheduled and a cancelled appointment for status variety.
        upsertAppointment(APPT_LENA_ASTHMA, LENA_ID, now.minusDays(50), 30,
                AppointmentStatus.COMPLETED, "Asthma review");
        upsertAppointment(APPT_LENA_ASTHMA_FOLLOWUP, LENA_ID, now.minusDays(15), 30,
                AppointmentStatus.COMPLETED, "Asthma follow-up");
        upsertAppointment(APPT_LENA_RESCHEDULED, LENA_ID, now.plusDays(8), 30,
                AppointmentStatus.RESCHEDULED, "Spirometry review");
        upsertAppointment(APPT_LENA_CANCELLED, LENA_ID, now.minusDays(5), 30,
                AppointmentStatus.CANCELLED, "Consultation");

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

        log.info("Demo dataset seeded (patient-service): 16 appointments, 9 open slots across 4 doctors");
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
        appointment.setPatientEmail(PATIENT_EMAILS.get(patientId));
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
