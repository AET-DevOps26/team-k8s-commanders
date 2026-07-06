package com.caredesk.patient.controller;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.repository.AppointmentRepository;
import org.openapitools.model.AppointmentStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service-to-service feed of appointments due soon, outside the public OpenAPI
 * contract.
 *
 * <p>notification-service's reminder scheduler polls
 * {@code GET /internal/appointments/upcoming} to find appointments that need a
 * reminder. The {@code /internal} prefix is not routed by the API gateway (it
 * only forwards the public {@code /appointments/**} paths), so this endpoint is
 * reachable only pod-to-pod, guarded by the cluster NetworkPolicy (only the
 * gateway, ai-assistant and notification-service may reach this service) and
 * permitted anonymously in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/internal/appointments")
public class InternalAppointmentsController {

    /** Active statuses that should still receive a reminder. */
    private static final List<AppointmentStatus> ACTIVE =
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.RESCHEDULED);

    private final AppointmentRepository appointmentRepository;

    /**
     * @param appointmentRepository repository for appointment rows
     */
    public InternalAppointmentsController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Returns active appointments starting within the next {@code withinHours}
     * hours.
     *
     * @param withinHours look-ahead window in hours (default 24)
     * @return the matching appointments as a lightweight feed
     */
    @GetMapping("/upcoming")
    public List<UpcomingAppointment> upcoming(
            @RequestParam(name = "withinHours", defaultValue = "24") int withinHours) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime until = now.plusHours(withinHours);
        return appointmentRepository.findByStatusInAndDateTimeBetween(ACTIVE, now, until).stream()
                // Skip appointments with no deliverable address: otherwise the
                // scheduler records a REMINDER for them (marking them reminded)
                // without ever sending mail, so they'd never be retried if an
                // address became available later.
                .filter(a -> a.getPatientEmail() != null && !a.getPatientEmail().isBlank())
                .map(InternalAppointmentsController::toFeedItem)
                .toList();
    }

    private static UpcomingAppointment toFeedItem(Appointment a) {
        return new UpcomingAppointment(a.getId(), a.getPatientId(), a.getDateTime(), a.getPatientEmail());
    }
}
