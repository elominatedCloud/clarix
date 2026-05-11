package com.clarix.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.UserRepository;

@Service
public class ReceptionService {

    private final UserRepository users;
    private final PatientService patientSvc;

    public ReceptionService(UserRepository users, PatientService patientSvc) {
        this.users = users;
        this.patientSvc = patientSvc;
    }

    public record ReceptionRow(
        UUID id, String name,
        int daysToRefill,           // -1 = no active prescription
        LocalDate nextRefillDate,   // null = no active prescription
        OffsetDateTime bookedAt,    // null = not booked
        String memo                 // can be null
    ) {
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
    }

    public List<ReceptionRow> patientsForHospital(UUID hospitalId) {
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
    public void toggleBooked(UUID staffHospitalId, UUID patientId, boolean booked) {
        User p = users.findById(patientId).orElseThrow();
        if (p.getHospital() == null || !p.getHospital().getId().equals(staffHospitalId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "다른 병원의 환자입니다");
        }
        p.setBookedAt(booked ? OffsetDateTime.now() : null);
        users.save(p);
    }
}
