package com.clarix.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.MedicationLog;

public interface MedicationLogRepository extends JpaRepository<MedicationLog, UUID> {
    List<MedicationLog> findByPatientIdAndTakenAtBetween(UUID patientId, OffsetDateTime from, OffsetDateTime to);
    List<MedicationLog> findByPatientIdAndTakenAtGreaterThanEqual(UUID patientId, OffsetDateTime since);
    Optional<MedicationLog> findFirstByPatientIdAndMedicationNameAndTakenAtBetween(
        UUID patientId, String medicationName, OffsetDateTime from, OffsetDateTime to
    );
    List<MedicationLog> findByPatientIdInAndTakenAtGreaterThanEqual(List<UUID> patientIds, OffsetDateTime since);
}
