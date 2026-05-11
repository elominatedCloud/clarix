package com.clarix.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.Assessment;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    Optional<Assessment> findFirstByPatientIdAndKindOrderByCreatedAtDesc(UUID patientId, String kind);
    List<Assessment> findByPatientIdAndKindOrderByCreatedAtAsc(UUID patientId, String kind);
    List<Assessment> findByPatientIdInAndKindOrderByCreatedAtDesc(List<UUID> patientIds, String kind);
}
