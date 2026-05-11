package com.clarix.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.ClinicalNote;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {
    List<ClinicalNote> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<ClinicalNote> findByDoctorIdAndPatientIdInOrderByCreatedAtDesc(UUID doctorId, List<UUID> patientIds);
}
