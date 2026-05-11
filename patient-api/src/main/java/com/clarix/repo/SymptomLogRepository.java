package com.clarix.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.SymptomLog;

public interface SymptomLogRepository extends JpaRepository<SymptomLog, UUID> {
    Optional<SymptomLog> findByPatientIdAndLogDate(UUID patientId, LocalDate logDate);
    List<SymptomLog> findByPatientIdAndLogDateGreaterThanEqualOrderByLogDateAsc(UUID patientId, LocalDate since);
    List<SymptomLog> findByPatientIdAndLogDateBetweenOrderByLogDateAsc(UUID patientId, LocalDate from, LocalDate to);
    List<SymptomLog> findByPatientIdInAndLogDateGreaterThanEqual(List<UUID> patientIds, LocalDate since);
}
