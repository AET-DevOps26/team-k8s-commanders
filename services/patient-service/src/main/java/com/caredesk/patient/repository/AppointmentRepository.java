package com.caredesk.patient.repository;

import com.caredesk.patient.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link Appointment}.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Returns every appointment booked for the given patient, in no specific
     * order.
     *
     * @param patientId the patient's user id from auth-service
     * @return list of matching appointments, possibly empty
     */
    List<Appointment> findByPatientId(UUID patientId);

    /**
     * Paged variant of {@link #findByPatientId(UUID)} used by the patient
     * profile API. Order is whatever the {@link Pageable} specifies, defaulting
     * to insertion order.
     *
     * @param patientId the patient's user id from auth-service
     * @param pageable  paging and sorting instructions
     * @return a page of matching appointments
     */
    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);

    /**
     * Returns every appointment scheduled against the given doctor, in no
     * specific order.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return list of matching appointments, possibly empty
     */
    List<Appointment> findByDoctorId(UUID doctorId);

    Optional<Appointment> findFirstByPatientIdAndDoctorIdAndReason(UUID patientId,
                                                                   UUID doctorId,
                                                                   String reason);
}
