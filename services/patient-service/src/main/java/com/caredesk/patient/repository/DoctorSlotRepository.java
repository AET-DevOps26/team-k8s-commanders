package com.caredesk.patient.repository;

import com.caredesk.patient.model.DoctorSlot;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select slot from DoctorSlot slot
            where slot.doctorId = :doctorId
              and slot.available = true
              and slot.startAt = :startAt
              and slot.endAt = :endAt
            """)
    Optional<DoctorSlot> findAndLockAvailableSlot(@Param("doctorId") UUID doctorId,
                                                  @Param("startAt") OffsetDateTime startAt,
                                                  @Param("endAt") OffsetDateTime endAt);

    @Query("""
            select slot from DoctorSlot slot
            where slot.doctorId = :doctorId
              and slot.startAt = :startAt
              and slot.endAt = :endAt
            """)
    Optional<DoctorSlot> findSlotByTime(@Param("doctorId") UUID doctorId,
                                        @Param("startAt") OffsetDateTime startAt,
                                        @Param("endAt") OffsetDateTime endAt);
}
