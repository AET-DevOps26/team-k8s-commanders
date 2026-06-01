package com.caredesk.patient.repository;

import com.caredesk.patient.model.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link DoctorSlot}.
 */
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, UUID> {

    /**
     * Returns every slot belonging to a doctor, including booked ones.
     *
     * @param doctorId the doctor's user id from auth-service
     * @return list of slots, possibly empty
     */
    List<DoctorSlot> findByDoctorId(UUID doctorId);
}
