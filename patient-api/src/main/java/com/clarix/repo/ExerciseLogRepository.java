package com.clarix.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.ExerciseLog;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, UUID> {
    List<ExerciseLog> findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(
        UUID patientId, OffsetDateTime since);
}
