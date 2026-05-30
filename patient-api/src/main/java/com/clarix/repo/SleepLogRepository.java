package com.clarix.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.SleepLog;

public interface SleepLogRepository extends JpaRepository<SleepLog, UUID> {

    Optional<SleepLog> findFirstByPatientIdOrderByLoggedAtDesc(UUID patientId);

    List<SleepLog> findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(
        UUID patientId, OffsetDateTime since);
}
