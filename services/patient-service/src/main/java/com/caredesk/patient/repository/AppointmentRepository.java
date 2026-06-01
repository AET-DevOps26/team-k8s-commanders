package com.caredesk.patient.repository;

import com.caredesk.patient.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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
     * Returns every appointment scheduled against the given doctor, in no
     * specific order.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return list of matching appointments, possibly empty
     */
    List<Appointment> findByDoctorId(UUID doctorId);
}
