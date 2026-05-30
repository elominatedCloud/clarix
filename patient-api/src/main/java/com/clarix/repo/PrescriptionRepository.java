package com.clarix.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarix.domain.Prescription;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByPatientIdAndActiveTrueOrderByCreatedAtAsc(UUID patientId);
    List<Prescription> findByPatientIdAndActiveFalseOrderByCreatedAtDesc(UUID patientId);
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    // 접수처(reception) 화면에서 병원 단위로 활성 처방을 모아 보기 위해 사용.
    List<Prescription> findByPatient_Hospital_IdAndActiveTrueOrderByCreatedAtDesc(UUID hospitalId);
}
