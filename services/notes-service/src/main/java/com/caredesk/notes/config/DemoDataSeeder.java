package com.caredesk.notes.config;

import com.caredesk.notes.model.ClinicalNote;
import com.caredesk.notes.model.Diagnosis;
import com.caredesk.notes.repository.ClinicalNoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Seeds clinical notes for the demo dataset (issue #168) so the doctor's
 * patient records and the patient's visit history are populated, and the AI
 * assistant has grounded content to summarise.
 *
 * <p>Enabled only in the {@code dev} profile — the single switch for all demo
 * seeding; never runs in production. Idempotent — one note per appointment,
 * keyed on appointmentId.
 *
 * <p>The appointment and doctor ids below mirror patient-service's demo seeder;
 * notes are written only for the appointments that are already {@code COMPLETED}
 * there. Keep these constants in sync with that seeder.
 */
@Component
@Profile("dev")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final UUID DOCTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID APPT_HTN_PAST = UUID.fromString("d0000000-0000-0000-0000-000000000101");
    private static final UUID APPT_BP_FOLLOWUP = UUID.fromString("d0000000-0000-0000-0000-000000000102");
    private static final UUID APPT_ANNA_DM_1 = UUID.fromString("d0000000-0000-0000-0000-000000000106");
    private static final UUID APPT_ANNA_DM_2 = UUID.fromString("d0000000-0000-0000-0000-000000000107");
    private static final UUID APPT_MAX_CHECKUP = UUID.fromString("d0000000-0000-0000-0000-000000000109");
    private static final UUID APPT_MAX_BACK_PAIN = UUID.fromString("d0000000-0000-0000-0000-00000000010a");
    private static final UUID APPT_LENA_ASTHMA = UUID.fromString("d0000000-0000-0000-0000-00000000010d");
    private static final UUID APPT_LENA_ASTHMA_FOLLOWUP = UUID.fromString("d0000000-0000-0000-0000-00000000010e");

    private final ClinicalNoteRepository noteRepository;

    /**
     * @param noteRepository clinical note rows
     */
    public DemoDataSeeder(ClinicalNoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    /**
     * Upserts one clinical note per completed demo appointment.
     *
     * @param args ignored
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsertNote(APPT_HTN_PAST,
                "Patient presented with elevated blood pressure (150/95). Started on "
                        + "amlodipine 5mg daily. Advised lifestyle changes and follow-up in 2 weeks.",
                "I10", "Essential (primary) hypertension");
        upsertNote(APPT_BP_FOLLOWUP,
                "Blood pressure improved to 132/84 on amlodipine. Continue current dose. "
                        + "Review again in 4 weeks.",
                "I10", "Essential (primary) hypertension");
        upsertNote(APPT_ANNA_DM_1,
                "Routine review of Type 2 diabetes. HbA1c 7.8%. Reinforced diet and metformin "
                        + "adherence. Next follow-up in 6 weeks.",
                "E11.9", "Type 2 diabetes mellitus without complications");
        upsertNote(APPT_ANNA_DM_2,
                "Follow-up: HbA1c down to 7.1%. Good progress. Continue metformin 1000mg BD.",
                "E11.9", "Type 2 diabetes mellitus without complications");
        upsertNote(APPT_MAX_CHECKUP,
                "Routine general check-up. Vitals within normal range. No acute concerns. "
                        + "Advised annual review.",
                "Z00.0", "General adult medical examination");
        upsertNote(APPT_MAX_BACK_PAIN,
                "Reports lower back pain after lifting. No red-flag features. Prescribed NSAIDs "
                        + "and referred to physiotherapy.",
                "M54.5", "Low back pain");
        upsertNote(APPT_LENA_ASTHMA,
                "Asthma review. Occasional night-time symptoms. Inhaler technique checked. "
                        + "Continued on salbutamol as needed.",
                "J45.909", "Unspecified asthma, uncomplicated");
        upsertNote(APPT_LENA_ASTHMA_FOLLOWUP,
                "Follow-up: symptoms well controlled, no recent exacerbations. Continue current "
                        + "inhaler regimen. Review in 6 months.",
                "J45.909", "Unspecified asthma, uncomplicated");

        log.info("Demo dataset seeded (notes-service): 8 clinical notes");
    }

    private void upsertNote(UUID appointmentId, String content, String diagnosisCode, String diagnosisDescription) {
        ClinicalNote note = noteRepository.findByAppointmentId(appointmentId).orElseGet(ClinicalNote::new);
        note.setAppointmentId(appointmentId);
        note.setDoctorId(DOCTOR_ID);
        note.setContent(content);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setCode(diagnosisCode);
        diagnosis.setDescription(diagnosisDescription);
        note.setDiagnosis(diagnosis);

        noteRepository.save(note);
    }
}
