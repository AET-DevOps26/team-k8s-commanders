package com.caredesk.patient.service;

import com.caredesk.patient.model.DoctorSlot;
import org.openapitools.model.ScheduleSlot;
import org.springframework.stereotype.Component;

/**
 * Converts a JPA {@link DoctorSlot} entity into the API
 * {@link ScheduleSlot} DTO returned by the doctor schedule endpoints.
 *
 * <p>The DTO carries the slot id so clients can address individual slots
 * (e.g. delete), but omits the doctor id, which belongs on the wrapping
 * {@link org.openapitools.model.Schedule}.
 */
@Component
public class ScheduleSlotMapper {

    /**
     * Maps a persisted doctor slot into its API counterpart.
     *
     * @param entity the JPA entity
     * @return a new API DTO with the same id, time range and availability flag
     */
    public ScheduleSlot toApi(DoctorSlot entity) {
        return new ScheduleSlot(entity.getId(), entity.getStartAt(), entity.getEndAt(), entity.getAvailable());
    }
}
