package com.clarix.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.StaffInvite;

public interface StaffInviteRepository extends JpaRepository<StaffInvite, UUID> {
    List<StaffInvite> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
    Optional<StaffInvite> findFirstByEmailAndActiveTrueAndConsumedAtIsNullOrderByCreatedAtDesc(String email);
}
