package com.clarix.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.ClinicalNote;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.ClinicalNoteRepository;
import com.clarix.repo.UserRepository;

/**
 * 간호사 / 의료기사 차팅 전용 서비스.
 * 권한: 같은 병원의 환자에 대해 차팅 작성/조회.
 * (의사의 Permission 권한 모델과 별개 — 같은 병원 직원은 차팅 자유)
 */
@Service
public class StaffService {

    private final UserRepository users;
    private final ClinicalNoteRepository notes;

    public StaffService(UserRepository users, ClinicalNoteRepository notes) {
        this.users = users;
        this.notes = notes;
    }

    public List<User> patientsForHospital(UUID hospitalId) {
        return users.findByHospitalIdAndRole(hospitalId, Role.PATIENT);
    }

    public User requirePatientInSameHospital(User staff, UUID patientId) {
        if (staff.getHospital() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "병원이 설정되지 않음");
        }
        User p = users.findById(patientId).orElseThrow();
        if (p.getHospital() == null
            || !p.getHospital().getId().equals(staff.getHospital().getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "다른 병원의 환자");
        }
        return p;
    }

    public List<ClinicalNote> notesForPatient(UUID patientId) {
        return notes.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional
    public ClinicalNote createNote(User staff, UUID patientId,
                                   String s, String o, String a, String p) {
        User patient = requirePatientInSameHospital(staff, patientId);
        ClinicalNote n = new ClinicalNote();
        n.setDoctor(staff); // ClinicalNote.doctor field reused as author (legacy name)
        n.setPatient(patient);
        n.setSubjective(s); n.setObjective(o); n.setAssessment(a); n.setPlan(p);
        n.setKind(com.clarix.domain.NoteKind.forRole(staff.getRole()));
        return notes.save(n);
    }
}
