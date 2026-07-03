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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Appointment CRUD operations.
 *
 * <p>Implements the booking, listing, retrieval, rescheduling and
 * cancellation flows for the patient and appointment service. Booking and
 * rescheduling consume doctor slots; cancellation releases the occupied slot.
 *
 * <p>Every operation enforces per-record ownership against the {@link Caller}
 * (derived from the gateway-injected {@code X-User-*} headers): a
 * {@code PATIENT} may only operate on their own appointments, a {@code DOCTOR}
 * only on those where they are the treating doctor, and an {@code ADMIN} on
 * all (issue #172).
 */
@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final AppointmentMapper appointmentMapper;

    /**
     * @param appointmentRepository repository for appointment rows
     * @param appointmentMapper     converts JPA entities to API DTOs
     */
    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorSlotRepository doctorSlotRepository,
                              AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.doctorSlotRepository = doctorSlotRepository;
        this.appointmentMapper = appointmentMapper;
    }

    /**
     * Books a new appointment. The new row always starts in
     * {@link AppointmentStatus#SCHEDULED}.
     *
     * <p>A {@code PATIENT} may only book for themselves; doctors and admins may
     * book on any patient's behalf (the doctor booking flow).
     *
     * @param request the booking request from the caller
     * @param caller  the authenticated caller
     * @return the persisted appointment as an API DTO
     * @throws AccessDeniedException if a patient books for someone else
     */
    public org.openapitools.model.Appointment book(AppointmentCreate request, Caller caller) {
        if (caller.isPatient() && !caller.is(request.getPatientId())) {
            throw new AccessDeniedException("Patients can only book appointments for themselves");
        }
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
        entity.setStatus(AppointmentStatus.SCHEDULED);
        Appointment saved = appointmentRepository.save(entity);
        return appointmentMapper.toApi(saved);
    }

    /**
     * Returns one page of appointments, scoped to the caller: an {@code ADMIN}
     * sees the whole clinic, a {@code DOCTOR} only appointments where they are
     * the treating doctor. Patients are denied here — their appointments are
     * served by the patient-scoped {@code /patients/{id}/appointments} endpoint.
     *
     * @param page   zero-based page index
     * @param size   page size, must be at least 1
     * @param caller the authenticated caller
     * @return a {@link PaginatedAppointmentResponse} with appointments and
     *         paging metadata
     * @throws AccessDeniedException if a patient calls the global listing
     */
    @Transactional(readOnly = true)
    public PaginatedAppointmentResponse list(int page, int size, Caller caller) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> result;
        if (caller.isAdmin()) {
            result = appointmentRepository.findAll(pageable);
        } else if (caller.isDoctor()) {
            result = appointmentRepository.findByDoctorId(caller.userId(), pageable);
        } else {
            throw new AccessDeniedException(
                    "Patients can list their own appointments via /patients/{patientId}/appointments");
        }
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
     * Returns a single appointment by id. Only the appointment's patient, its
     * treating doctor or an admin may read it.
     *
     * @param id     the appointment id
     * @param caller the authenticated caller
     * @return the appointment as an API DTO
     * @throws AppointmentNotFoundException if no appointment exists with that id
     * @throws AccessDeniedException        if the caller is not a participant or admin
     */
    @Transactional(readOnly = true)
    public org.openapitools.model.Appointment getById(UUID id, Caller caller) {
        Appointment entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        requireParticipantOrAdmin(entity, caller);
        return appointmentMapper.toApi(entity);
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
    public org.openapitools.model.Appointment reschedule(UUID id, AppointmentRescheduleRequest request, Caller caller) {
        Appointment entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        requireParticipantOrAdmin(entity, caller);
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
        return appointmentMapper.toApi(appointmentRepository.save(entity));
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
    public org.openapitools.model.Appointment cancel(UUID id, Caller caller) {
        Appointment entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        requireParticipantOrAdmin(entity, caller);
        if (entity.getStatus() != AppointmentStatus.CANCELLED) {
            rejectPastAppointment(entity, "cancelled");
            releaseSlot(entity.getDoctorId(), entity.getDateTime(), entity.getDuration());
            entity.setStatus(AppointmentStatus.CANCELLED);
            entity = appointmentRepository.save(entity);
        }
        return appointmentMapper.toApi(entity);
    }

    /**
     * Rejects callers that are neither a participant of the appointment (its
     * patient or treating doctor) nor an admin. This is the per-record
     * ownership rule behind read, reschedule and cancel.
     *
     * @param appointment the loaded appointment
     * @param caller      the authenticated caller
     * @throws AccessDeniedException if the caller may not touch this appointment
     */
    private void requireParticipantOrAdmin(Appointment appointment, Caller caller) {
        if (caller.isAdmin() || caller.is(appointment.getPatientId()) || caller.is(appointment.getDoctorId())) {
            return;
        }
        throw new AccessDeniedException("Not your appointment");
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
