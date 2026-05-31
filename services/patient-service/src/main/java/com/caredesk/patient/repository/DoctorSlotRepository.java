package com.caredesk.patient.repository;

import com.caredesk.patient.model.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, UUID> {
    List<DoctorSlot> findByDoctorId(UUID doctorId);
}
