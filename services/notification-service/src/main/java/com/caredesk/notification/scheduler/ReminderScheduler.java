package com.caredesk.notification.scheduler;

import com.caredesk.notification.client.PatientServiceClient;
import com.caredesk.notification.client.UpcomingAppointment;
import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.repository.NotificationRepository;
import com.caredesk.notification.service.NotificationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Periodically sends reminder emails for appointments due soon.
 *
 * <p>On each tick the scheduler asks patient-service for appointments starting
 * within the configured window (default 24h) and, for any that have not been
 * reminded yet, records and sends a reminder. Idempotency is enforced against
 * the persisted record ({@link NotificationType#REMINDER}) rather than
 * in-memory state, so an appointment is reminded at most once even if the
 * service restarts between ticks.
 *
 * <p>The whole scan is best-effort: a reminder that fails for one appointment
 * is logged and skipped without aborting the rest of the tick. Disable with
 * {@code notification.reminder.enabled=false} (used in tests).
 */
@Component
@ConditionalOnProperty(value = "notification.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter WHEN_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm 'UTC'");

    private final PatientServiceClient patientServiceClient;
    private final NotificationsService notificationsService;
    private final NotificationRepository repository;
    private final int windowHours;

    /**
     * @param patientServiceClient client for the upcoming-appointments feed
     * @param notificationsService notification record + delivery logic
     * @param repository           used to check whether a reminder already exists
     * @param windowHours          how far ahead to look for appointments to remind
     */
    public ReminderScheduler(PatientServiceClient patientServiceClient,
                             NotificationsService notificationsService,
                             NotificationRepository repository,
                             @Value("${notification.reminder.window-hours:24}") int windowHours) {
        this.patientServiceClient = patientServiceClient;
        this.notificationsService = notificationsService;
        this.repository = repository;
        this.windowHours = windowHours;
    }

    /**
     * Scans for appointments due within the window and sends any missing
     * reminders. Scheduled with a fixed delay so a slow scan never overlaps the
     * next one.
     */
    @Scheduled(
            fixedDelayString = "${notification.reminder.scan-interval-ms:900000}",
            initialDelayString = "${notification.reminder.initial-delay-ms:60000}")
    public void sendDueReminders() {
        var upcoming = patientServiceClient.fetchUpcoming(windowHours);
        if (upcoming.isEmpty()) {
            return;
        }
        int sent = 0;
        for (UpcomingAppointment appt : upcoming) {
            try {
                if (repository.existsByAppointmentIdAndType(appt.appointmentId(), NotificationType.REMINDER)) {
                    continue;
                }
                notificationsService.recordAndSend(
                        appt.appointmentId(),
                        appt.patientId(),
                        appt.recipientEmail(),
                        NotificationType.REMINDER,
                        "Appointment reminder",
                        reminderBody(appt));
                sent++;
            } catch (RuntimeException e) {
                // One bad appointment must not abort the rest of the scan.
                log.error("Failed to send reminder for appointment {}: {}",
                        appt.appointmentId(), e.getMessage());
            }
        }
        if (sent > 0) {
            log.info("Reminder scan sent {} reminder(s)", sent);
        }
    }

    private static String reminderBody(UpcomingAppointment appt) {
        // Convert to UTC before formatting — WHEN_FORMAT hard-codes the "UTC"
        // label, so a non-UTC offset would otherwise be mislabelled.
        String when = appt.dateTime() != null
                ? WHEN_FORMAT.format(appt.dateTime().withOffsetSameInstant(ZoneOffset.UTC))
                : "soon";
        return "This is a reminder that you have an upcoming appointment on " + when
                + ". Please contact the clinic if you need to reschedule.";
    }
}
