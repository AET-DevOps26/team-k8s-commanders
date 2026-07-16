package com.caredesk.notification.scheduler;

import com.caredesk.notification.client.PatientServiceClient;
import com.caredesk.notification.client.UpcomingAppointment;
import com.caredesk.notification.config.RequestLoggingFilter;
import com.caredesk.notification.model.Notification;
import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.repository.NotificationRepository;
import com.caredesk.notification.service.NotificationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Periodically sends reminder emails for appointments due soon.
 *
 * <p>On each tick the scheduler asks patient-service for appointments starting
 * within the configured window (default 24h) and sends a reminder for each.
 * There is at most one {@link NotificationType#REMINDER} record per appointment
 * (upserted in place), so:
 * <ul>
 *   <li>an appointment with no deliverable email is skipped entirely — no record
 *       is created, so no-email patients never accumulate rows;</li>
 *   <li>a reminder that has already been delivered is never sent again (checked
 *       against the persisted record, so the guarantee holds across restarts);</li>
 *   <li>a failing send is retried on later ticks, but only up to
 *       {@code max-attempts} — an undeliverable appointment doesn't re-attempt
 *       forever.</li>
 * </ul>
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
    private final int maxAttempts;

    /**
     * @param patientServiceClient client for the upcoming-appointments feed
     * @param notificationsService notification record + delivery logic
     * @param repository           used to look up the existing reminder record
     * @param windowHours          how far ahead to look for appointments to remind
     * @param maxAttempts          how many times to retry an undelivered reminder
     *                             before giving up
     */
    public ReminderScheduler(PatientServiceClient patientServiceClient,
                             NotificationsService notificationsService,
                             NotificationRepository repository,
                             @Value("${notification.reminder.window-hours:24}") int windowHours,
                             @Value("${notification.reminder.max-attempts:5}") int maxAttempts) {
        this.patientServiceClient = patientServiceClient;
        this.notificationsService = notificationsService;
        this.repository = repository;
        this.windowHours = windowHours;
        this.maxAttempts = maxAttempts;
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
        try (MDC.MDCCloseable ignored = MDC.putCloseable(
                RequestLoggingFilter.MDC_KEY, UUID.randomUUID().toString())) {
            scanDueReminders();
        }
    }

    private void scanDueReminders() {
        var upcoming = patientServiceClient.fetchUpcoming(windowHours);
        if (upcoming.isEmpty()) {
            log.info("Reminder scan completed candidates=0 remindersSent=0");
            return;
        }
        int sent = 0;
        for (UpcomingAppointment appt : upcoming) {
            try {
                // No deliverable address → nothing to send, and recording an
                // undeliverable reminder would just accumulate dead rows.
                if (!StringUtils.hasText(appt.recipientEmail())) {
                    continue;
                }
                Optional<Notification> existing = repository.findFirstByAppointmentIdAndType(
                        appt.appointmentId(), NotificationType.REMINDER);
                if (existing.isPresent()
                        && (existing.get().isDelivered() || existing.get().getDeliveryAttempts() >= maxAttempts)) {
                    // Already delivered, or retries exhausted — leave it alone.
                    continue;
                }
                notificationsService.recordAndSendReminder(
                        appt.appointmentId(),
                        appt.patientId(),
                        appt.recipientEmail(),
                        "Appointment reminder",
                        reminderBody(appt));
                sent++;
            } catch (RuntimeException e) {
                // One bad appointment must not abort the rest of the scan.
                log.error("Failed to send reminder for appointment {}: {}",
                        appt.appointmentId(), e.getMessage());
            }
        }
        log.info("Reminder scan completed candidates={} remindersSent={}", upcoming.size(), sent);
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
