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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @param request the booking request from the caller
     * @return the persisted appointment as an API DTO
     */
    public org.openapitools.model.Appointment book(AppointmentCreate request) {
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
     * are left untouched. Cancelled appointments cannot be rescheduled.
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
     * already-cancelled appointment returns it unchanged.
     *
     * @param id the appointment id
     * @return the cancelled appointment as an API DTO
     * @throws AppointmentNotFoundException if no appointment exists with that id
     */
    public org.openapitools.model.Appointment cancel(UUID id) {
        Appointment entity = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        if (entity.getStatus() != AppointmentStatus.CANCELLED) {
            releaseSlot(entity.getDoctorId(), entity.getDateTime(), entity.getDuration());
            entity.setStatus(AppointmentStatus.CANCELLED);
            entity = appointmentRepository.save(entity);
        }
        return appointmentMapper.toApi(entity);
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
