package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import org.openapitools.model.ScheduleSlot;
import org.springframework.stereotype.Component;

/**
 * Converts a JPA {@link DoctorSlot} entity into the API
 * {@link ScheduleSlot} DTO returned by the doctor schedule endpoint.
 *
 * <p>The API DTO intentionally omits the slot id and the doctor id, those
 * belong on the wrapping {@link org.openapitools.model.Schedule} or on the
 * underlying entity but are not part of an individual slot in the contract.
 */
@Component
public class ScheduleSlotMapper {

    /**
     * Maps a persisted doctor slot into its API counterpart.
     *
     * @param entity the JPA entity
     * @return a new API DTO with the same time range and availability flag
     */
    public ScheduleSlot toApi(DoctorSlot entity) {
        return new ScheduleSlot(entity.getStartAt(), entity.getEndAt(), entity.getAvailable());
    }
}
