package com.caredesk.notification.config;

import com.caredesk.notification.model.Notification;
import com.caredesk.notification.repository.NotificationRepository;
import org.openapitools.model.NotificationChannel;
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
 * Seeds notification records for the demo dataset (issue #168) so the
 * notifications API and any patient/admin notification views return real data
 * out of the box.
 *
 * <p>Enabled only in the {@code dev} profile — the single switch for all demo
 * seeding; never runs in production. Idempotent — each record is keyed on a
 * fixed UUID.
 *
 * <p>The patient and appointment ids mirror patient-service's demo seeder; keep
 * them in sync. Records are stamped as already sent (email delivery + the
 * reminder scheduler are a separate iteration).
 */
@Component
@Profile("dev")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANNA_ID = UUID.fromString("d0000000-0000-0000-0000-0000000000a1");
    private static final UUID APPT_UPCOMING_24H = UUID.fromString("d0000000-0000-0000-0000-000000000103");
    private static final UUID APPT_RESCHEDULED = UUID.fromString("d0000000-0000-0000-0000-000000000104");
    private static final UUID APPT_ANNA_UPCOMING = UUID.fromString("d0000000-0000-0000-0000-000000000108");

    // Fixed notification ids (…02NN) for idempotency.
    private static final UUID NOTIF_CONFIRM_UPCOMING = UUID.fromString("d0000000-0000-0000-0000-000000000201");
    private static final UUID NOTIF_REMINDER_UPCOMING = UUID.fromString("d0000000-0000-0000-0000-000000000202");
    private static final UUID NOTIF_CONFIRM_RESCHEDULED = UUID.fromString("d0000000-0000-0000-0000-000000000203");
    private static final UUID NOTIF_CONFIRM_ANNA = UUID.fromString("d0000000-0000-0000-0000-000000000204");

    private final NotificationRepository notificationRepository;

    /**
     * @param notificationRepository notification rows
     */
    public DemoDataSeeder(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Upserts a small set of confirmation / reminder notification records.
     *
     * @param args ignored
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        upsert(NOTIF_CONFIRM_UPCOMING, APPT_UPCOMING_24H, PATIENT_ID, now.minusDays(6),
                "Your appointment 'Annual check-up' is confirmed.");
        upsert(NOTIF_REMINDER_UPCOMING, APPT_UPCOMING_24H, PATIENT_ID, now.minusHours(12),
                "Reminder: you have an appointment 'Annual check-up' in the next 24 hours.");
        upsert(NOTIF_CONFIRM_RESCHEDULED, APPT_RESCHEDULED, PATIENT_ID, now.minusDays(1),
                "Your appointment has been rescheduled.");
        upsert(NOTIF_CONFIRM_ANNA, APPT_ANNA_UPCOMING, ANNA_ID, now.minusDays(2),
                "Your appointment 'Diabetes review' is confirmed.");

        log.info("Demo dataset seeded (notification-service): 4 notification records");
    }

    private void upsert(UUID id, UUID appointmentId, UUID patientId, OffsetDateTime sentAt, String message) {
        Notification notification = notificationRepository.findById(id).orElseGet(Notification::new);
        notification.setId(id);
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(patientId);
        notification.setMessage(message);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setSentAt(sentAt);
        notificationRepository.save(notification);
    }
}
