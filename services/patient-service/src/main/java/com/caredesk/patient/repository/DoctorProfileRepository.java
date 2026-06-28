package com.caredesk.patient.repository;

import com.caredesk.patient.model.DoctorProfile;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select doctor from DoctorProfile doctor where doctor.id = :doctorId")
    Optional<DoctorProfile> findByIdForUpdate(@Param("doctorId") UUID doctorId);

    @Query("""
            select doctor from DoctorProfile doctor
            where (:q is null
                   or lower(doctor.name) like lower(concat('%', :q, '%'))
                   or lower(doctor.specialization) like lower(concat('%', :q, '%')))
              and (:specialization is null
                   or lower(doctor.specialization) like lower(concat('%', :specialization, '%')))
            order by doctor.name asc
            """)
    Page<DoctorProfile> search(@Param("q") String q,
                               @Param("specialization") String specialization,
                               Pageable pageable);
}
