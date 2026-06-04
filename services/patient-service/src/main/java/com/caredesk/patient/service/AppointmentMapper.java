package com.caredesk.patient.service;

import com.caredesk.patient.model.Appointment;
import org.springframework.stereotype.Component;

/**
 * Converts the JPA {@link Appointment} entity into the API-shaped
 * {@link org.openapitools.model.Appointment} returned to callers.
 *
 * <p>The two types are intentionally separate so the persistence model can
 * evolve independently of the API contract.
 */
@Component
public class AppointmentMapper {

    /**
     * Maps a persistence-layer appointment into its API counterpart.
     *
     * @param entity the JPA entity
     * @return a new API DTO with the same field values
     */
    public org.openapitools.model.Appointment toApi(Appointment entity) {
        org.openapitools.model.Appointment dto = new org.openapitools.model.Appointment(
                entity.getId(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getDateTime(),
                entity.getStatus(),
                entity.getDuration()
        );
        dto.setReason(entity.getReason());
        return dto;
    }
}
