package com.clarix.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.CareRequest;
import com.clarix.domain.CareRequestStatus;

public interface CareRequestRepository extends JpaRepository<CareRequest, UUID> {
    List<CareRequest> findByHospitalIdAndStatusOrderByCreatedAtAsc(UUID hospitalId, CareRequestStatus status);
    List<CareRequest> findTop5ByPatientIdOrderByCreatedAtDesc(UUID patientId);
    Optional<CareRequest> findFirstByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, CareRequestStatus status);
}
