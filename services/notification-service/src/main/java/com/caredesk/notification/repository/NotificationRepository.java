package com.caredesk.notification.repository;

import com.caredesk.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for {@link Notification}.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns one page of notifications addressed to the given patient.
     * Backs the patient-scoped view of {@code GET /notifications}.
     *
     * @param patientId the recipient patient's user id from auth-service
     * @param pageable  page request
     * @return the matching page, possibly empty
     */
    Page<Notification> findByPatientId(UUID patientId, Pageable pageable);

    /**
     * Returns one page of notifications tied to the given appointment.
     * Backs the admin view of {@code GET /appointments/{appointmentId}/notifications}.
     *
     * @param appointmentId the appointment id
     * @param pageable      page request
     * @return the matching page, possibly empty
     */
    Page<Notification> findByAppointmentId(UUID appointmentId, Pageable pageable);

    /**
     * Returns one page of notifications tied to the given appointment AND
     * addressed to the given patient. Backs the patient-scoped view of
     * {@code GET /appointments/{appointmentId}/notifications} so patients
     * cannot read notifications about other patients' appointments.
     *
     * @param appointmentId the appointment id
     * @param patientId     the recipient patient's user id
     * @param pageable      page request
     * @return the matching page, possibly empty
     */
    Page<Notification> findByAppointmentIdAndPatientId(UUID appointmentId, UUID patientId, Pageable pageable);
}
