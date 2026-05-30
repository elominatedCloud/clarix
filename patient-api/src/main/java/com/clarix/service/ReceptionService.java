package com.clarix.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Prescription;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.PrescriptionRepository;
import com.clarix.repo.UserRepository;

@Service
public class ReceptionService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Seoul");

    private final UserRepository users;
    private final PatientService patientSvc;
    private final PrescriptionRepository prescriptions;

    public ReceptionService(UserRepository users, PatientService patientSvc,
                            PrescriptionRepository prescriptions) {
        this.users = users;
        this.patientSvc = patientSvc;
        this.prescriptions = prescriptions;
    }

    /** 같은 병원 환자들의 활성 처방을 최신순으로 가져옵니다. 접수처 처방전 발행 화면 전용. */
    public List<Prescription> activePrescriptionsForHospital(UUID hospitalId) {
        return prescriptions.findByPatient_Hospital_IdAndActiveTrueOrderByCreatedAtDesc(hospitalId);
    }

    /** 특정 환자의 활성 처방. */
    public List<Prescription> activePrescriptionsForPatient(UUID patientId) {
        return prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId);
    }

    /** 환자가 접수처와 같은 병원 소속인지 확인하고 User 반환. 다르면 403. */
    public User requireSameHospitalPatient(UUID hospitalId, UUID patientId) {
        User patient = users.findById(patientId).orElseThrow();
        if (patient.getHospital() == null
            || !patient.getHospital().getId().equals(hospitalId)
            || patient.getRole() != Role.PATIENT) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "다른 병원의 환자입니다.");
        }
        return patient;
    }

    public record ReceptionRow(
        UUID id, String name,
        int daysToRefill,           // -1 = no active prescription
        LocalDate nextRefillDate,   // null = no active prescription
        OffsetDateTime bookedAt,    // null = not booked
        String memo                 // can be null
    ) {
        private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
        private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        public String urgencyTone() {
            if (bookedAt != null) return "ok";
            if (daysToRefill < 0) return "muted";
            if (daysToRefill <= 3)  return "danger";
            if (daysToRefill <= 7)  return "warn";
            return "muted";
        }
        public String urgencyLabel() {
            if (bookedAt != null) return "예약됨";
            if (daysToRefill < 0) return "처방 없음";
            if (daysToRefill <= 3)  return "긴급";
            if (daysToRefill <= 7)  return "임박";
            return "여유";
        }

        public String bookedAtInputValue() {
            if (bookedAt == null) return "";
            return bookedAt.atZoneSameInstant(CLINIC_ZONE)
                .toLocalDateTime()
                .format(INPUT_FORMAT);
        }

        public String bookedAtDisplayValue() {
            if (bookedAt == null) return "";
            return bookedAt.atZoneSameInstant(CLINIC_ZONE).format(DISPLAY_FORMAT);
        }
    }

    public List<ReceptionRow> patientsForHospital(UUID hospitalId) {
        // 데스크 화면은 "처방 종료가 임박했는데 아직 예약 안 된 환자"를 위로 올리는 업무용 목록입니다.
        var patients = users.findByHospitalIdAndRole(hospitalId, Role.PATIENT);
        return patients.stream().map(p -> new ReceptionRow(
                p.getId(),
                p.getName(),
                patientSvc.daysUntilRefill(p.getId()),
                patientSvc.nextRefillDate(p.getId()).orElse(null),
                p.getBookedAt(),
                p.getReceptionMemo()
            ))
            .sorted(Comparator
                // 미예약을 위쪽
                .<ReceptionRow, Integer>comparing(r -> r.bookedAt() == null ? 0 : 1)
                // refill 가까운 순
                .thenComparingInt(r -> r.daysToRefill() < 0 ? Integer.MAX_VALUE : r.daysToRefill()))
            .toList();
    }

    @Transactional
    public void updateMemo(UUID staffHospitalId, UUID patientId, String memo) {
        User p = users.findById(patientId).orElseThrow();
        if (p.getHospital() == null || !p.getHospital().getId().equals(staffHospitalId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "다른 병원의 환자입니다");
        }
        p.setReceptionMemo(memo == null || memo.isBlank() ? null : memo.trim());
        users.save(p);
    }

    @Transactional
    public void toggleBooked(UUID staffHospitalId, UUID patientId,
                             boolean booked, LocalDateTime appointmentAt) {
        User p = users.findById(patientId).orElseThrow();
        if (p.getHospital() == null || !p.getHospital().getId().equals(staffHospitalId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "다른 병원의 환자입니다");
        }
        p.setBookedAt(booked
            ? appointmentAt.atZone(CLINIC_ZONE).toOffsetDateTime()
            : null);
        users.save(p);
    }
}
