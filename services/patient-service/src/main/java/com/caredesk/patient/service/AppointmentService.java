package com.caredesk.patient.service;

import com.caredesk.patient.model.Appointment;
import com.caredesk.patient.model.DoctorSlot;
import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.openapitools.model.AppointmentCreate;
import org.openapitools.model.AppointmentRescheduleRequest;
import org.openapitools.model.AppointmentStatus;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedAppointmentResponse;
import org.openapitools.model.UserProfile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Appointment CRUD operations.
 *
 * <p>Implements the booking, listing, retrieval, rescheduling and
 * cancellation flows for the patient and appointment service. Booking and
 * rescheduling consume doctor slots; cancellation releases the occupied slot.
 *
 * <p>Authorisation is currently the same as the rest of the patient service,
 * any authenticated caller may invoke any endpoint. Per-role ownership rules
 * (a {@code PATIENT} may only operate on their own appointments, a
 * {@code DOCTOR} only on theirs) are tracked in issue #32.
 */
@Service
@Transactional
public class AppointmentService {

    private static final DateTimeFormatter WHEN_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm 'UTC'");

    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final AppointmentMapper appointmentMapper;
    private final AuthServiceClient authServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param appointmentRepository repository for appointment rows
     * @param appointmentMapper     converts JPA entities to API DTOs
     * @param authServiceClient     resolves the patient's contact email from the
     *                              authoritative user profile
     * @param eventPublisher        publishes notification triggers, dispatched
     *                              after commit by {@link AppointmentNotificationListener}
     */
    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorSlotRepository doctorSlotRepository,
                              AppointmentMapper appointmentMapper,
                              AuthServiceClient authServiceClient,
                              ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.doctorSlotRepository = doctorSlotRepository;
        this.appointmentMapper = appointmentMapper;
        this.authServiceClient = authServiceClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Books a new appointment. The new row always starts in
     * {@link AppointmentStatus#SCHEDULED}.
     *
     * <p>The <em>patient's</em> contact email is resolved from the authoritative
     * user profile by {@code patientId} (not from the caller, who may be a doctor
     * booking on the patient's behalf) and stored on the appointment so
     * notification-service can deliver the confirmation and any later reminder.
     * The confirmation notification is published and dispatched only after the
     * booking transaction commits.
     *
     * @param request the booking request from the caller
     * @return the persisted appointment as an API DTO
     */
    public org.openapitools.model.Appointment book(AppointmentCreate request) {
        rejectPastDateTime(request.getDateTime(), "booked");
        DoctorSlot slot = findAvailableSlot(request.getDoctorId(), request.getDateTime(), request.getDuration());
        slot.setAvailable(false);
        doctorSlotRepository.save(slot);

        Appointment entity = new Appointment();
        entity.setPatientId(request.getPatientId());
        entity.setDoctorId(request.getDoctorId());
        entity.setDateTime(request.getDateTime());
        entity.setDuration(request.getDuration());
        entity.setReason(request.getReason());
        entity.setPatientEmail(resolvePatientEmail(request.getPatientId()));
        entity.setStatus(AppointmentStatus.SCHEDULED);
        Appointment saved = appointmentRepository.save(entity);

        notify(saved, "CONFIRMATION", "Appointment confirmed",
                "Your appointment on " + formatWhen(saved.getDateTime()) + doctorSuffix(saved.getDoctorId())
                        + " is confirmed. We look forward to seeing you.");
        return appointmentMapper.toApi(saved);
    }

    /**
     * Returns one page of all appointments across the system.
     *
     * @param page zero-based page index
     * @param size page size, must be at least 1
     * @return a {@link PaginatedAppointmentResponse} with appointments and
     *         paging metadata
     */
    @Transactional(readOnly = true)
    public PaginatedAppointmentResponse list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> result = appointmentRepository.findAll(pageable);
        List<org.openapitools.model.Appointment> content = result.getContent().stream()
                .map(appointmentMapper::toApi)
                .toList();
        PageMeta meta = new PageMeta()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
        return new PaginatedAppointmentResponse(content, meta);
    }

    /**
     * Returns a single appointment by id.
     *
     * @param id the appointment id
     * @return the appointment as an API DTO
     * @throws AppointmentNotFoundException if no appointment exists with that id
     */
    @Transactional(readOnly = true)
    public org.openapitools.model.Appointment getById(UUID id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toApi)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    /**
     * Reschedules an existing appointment.
     *
     * <p>Updates the date / time and, if provided, the duration. Other fields
     * are left untouched. Cancelled, completed and past appointments cannot be
     * rescheduled.
     *
     * @param id      the appointment id
     * @param request the new date / time and optional duration
     * @return the updated appointment as an API DTO
     * @throws AppointmentNotFoundException       if no appointment exists with that id
     * @throws AppointmentStateConflictException  if the appointment is already cancelled
     */
    public org.openapitools.model.Appointment reschedule(UUID id, AppointmentRescheduleRequest request) {
        Appointment entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        if (entity.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentStateConflictException("Cancelled appointment cannot be rescheduled: " + id);
        }
        if (entity.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentStateConflictException("Completed appointment cannot be rescheduled: " + id);
        }
        rejectPastAppointment(entity, "rescheduled");
        int duration = request.getDuration() != null ? request.getDuration() : entity.getDuration();
        DoctorSlot newSlot = findAvailableSlot(entity.getDoctorId(), request.getDateTime(), duration);
        releaseSlot(entity.getDoctorId(), entity.getDateTime(), entity.getDuration());
        newSlot.setAvailable(false);
        doctorSlotRepository.save(newSlot);
        entity.setDateTime(request.getDateTime());
        entity.setDuration(duration);
        // Mark as RESCHEDULED so the patient and doctor dashboards can flag it.
        entity.setStatus(AppointmentStatus.RESCHEDULED);
        Appointment saved = appointmentRepository.save(entity);

        notify(saved, "RESCHEDULE", "Appointment rescheduled",
                "Your appointment" + doctorSuffix(saved.getDoctorId()) + " has been moved to "
                        + formatWhen(saved.getDateTime()) + ".");
        return appointmentMapper.toApi(saved);
    }

    /**
     * Cancels an appointment. The operation is idempotent, cancelling an
     * already-cancelled appointment returns it unchanged. Past appointments
     * cannot be cancelled.
     *
     * @param id the appointment id
     * @return the cancelled appointment as an API DTO
     * @throws AppointmentNotFoundException if no appointment exists with that id
     */
    public org.openapitools.model.Appointment cancel(UUID id) {
        Appointment entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        if (entity.getStatus() != AppointmentStatus.CANCELLED) {
            rejectPastAppointment(entity, "cancelled");
            releaseSlot(entity.getDoctorId(), entity.getDateTime(), entity.getDuration());
            entity.setStatus(AppointmentStatus.CANCELLED);
            entity = appointmentRepository.save(entity);

            notify(entity, "CANCELLATION", "Appointment cancelled",
                    "Your appointment on " + formatWhen(entity.getDateTime()) + doctorSuffix(entity.getDoctorId())
                            + " has been cancelled.");
        }
        return appointmentMapper.toApi(entity);
    }

    /**
     * Publishes a notification trigger for a booking event. The event is
     * dispatched by {@link AppointmentNotificationListener} only after the
     * surrounding transaction commits, so nothing is sent for a change that
     * rolled back. The recipient email is whatever was resolved and stored on
     * the appointment at booking.
     */
    private void notify(Appointment appt, String type, String subject, String message) {
        eventPublisher.publishEvent(new AppointmentNotificationEvent(new NotificationTriggerRequest(
                appt.getId(), appt.getPatientId(), appt.getPatientEmail(), type, subject, message)));
    }

    /**
     * Resolves the patient's contact email from the authoritative auth-service
     * profile. Returns {@code null} if the profile can't be reached or has no
     * email — the notification record is still created, just without delivery.
     */
    private String resolvePatientEmail(UUID patientId) {
        UserProfile profile = authServiceClient.getUserById(patientId);
        return profile != null ? profile.getEmail() : null;
    }

    /**
     * Best-effort " with Dr <name>" phrase for notification messages, resolved
     * from the authoritative doctor profile. Returns an empty string if the
     * profile can't be reached or has no name, so the message stays well-formed.
     */
    private String doctorSuffix(UUID doctorId) {
        UserProfile profile = authServiceClient.getUserById(doctorId);
        if (profile == null || profile.getName() == null || profile.getName().isBlank()) {
            return "";
        }
        return " with Dr " + profile.getName();
    }

    private static String formatWhen(OffsetDateTime when) {
        // Convert to UTC before formatting — WHEN_FORMAT hard-codes the "UTC"
        // label, so a non-UTC offset would otherwise be mislabelled.
        return when != null
                ? WHEN_FORMAT.format(when.withOffsetSameInstant(ZoneOffset.UTC))
                : "the scheduled time";
    }

    private void rejectPastAppointment(Appointment appointment, String operation) {
        if (isPast(appointment.getDateTime())) {
            throw new AppointmentStateConflictException("Past appointment cannot be " + operation + ": " + appointment.getId());
        }
    }

    private void rejectPastDateTime(OffsetDateTime dateTime, String operation) {
        if (isPast(dateTime)) {
            throw new AppointmentStateConflictException("Past appointment cannot be " + operation);
        }
    }

    private boolean isPast(OffsetDateTime dateTime) {
        return dateTime.toInstant().isBefore(OffsetDateTime.now().toInstant());
    }

    private DoctorSlot findAvailableSlot(UUID doctorId, java.time.OffsetDateTime startAt, int duration) {
        return doctorSlotRepository.findAndLockAvailableSlot(doctorId, startAt, startAt.plusMinutes(duration))
                .orElseThrow(() -> new AppointmentStateConflictException("Selected doctor slot is unavailable"));
    }

    private void releaseSlot(UUID doctorId, java.time.OffsetDateTime startAt, int duration) {
        doctorSlotRepository.findSlotByTime(doctorId, startAt, startAt.plusMinutes(duration))
                .ifPresent(slot -> {
                    slot.setAvailable(true);
                    doctorSlotRepository.save(slot);
                });
    }
}
