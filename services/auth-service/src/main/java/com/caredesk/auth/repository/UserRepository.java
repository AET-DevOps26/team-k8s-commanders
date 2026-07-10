package com.caredesk.auth.repository;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByEnabled(boolean enabled);

    @Query("""
            select u from User u
            where u.role = com.caredesk.auth.model.Role.DOCTOR
              and u.enabled = true
              and (:q = ''
                   or lower(u.name) like lower(concat('%', :q, '%'))
                   or lower(u.specialization) like lower(concat('%', :q, '%')))
              and (:specialization = ''
                   or lower(u.specialization) like lower(concat('%', :specialization, '%')))
            order by u.name asc
            """)
    Page<User> searchDoctors(@Param("q") String q,
                             @Param("specialization") String specialization,
                             Pageable pageable);

    /**
     * Distinct, alphabetically-sorted specializations across all enabled
     * doctors. Backs the booking flow's specialization filter — a new value
     * appears here as soon as an admin creates a doctor with it.
     */
    @Query("""
            select distinct u.specialization from User u
            where u.role = com.caredesk.auth.model.Role.DOCTOR
              and u.enabled = true
              and u.specialization is not null
              and u.specialization <> ''
            order by u.specialization asc
            """)
    List<String> findDistinctSpecializations();
}
