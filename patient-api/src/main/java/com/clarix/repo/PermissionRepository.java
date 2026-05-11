package com.clarix.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByDoctorIdAndActiveTrue(UUID doctorId);
    List<Permission> findByPatientId(UUID patientId);
    Optional<Permission> findByPatientIdAndDoctorId(UUID patientId, UUID doctorId);
    boolean existsByPatientIdAndDoctorIdAndActiveTrue(UUID patientId, UUID doctorId);
}
