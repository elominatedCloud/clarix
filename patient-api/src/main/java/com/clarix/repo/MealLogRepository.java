package com.clarix.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.MealLog;

public interface MealLogRepository extends JpaRepository<MealLog, UUID> {
    List<MealLog> findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(
        UUID patientId, OffsetDateTime since);
}
