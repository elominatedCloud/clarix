package com.clarix.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Assessment;
import com.clarix.domain.Severity;
import com.clarix.domain.User;
import com.clarix.repo.AssessmentRepository;

@Service
public class AssessmentService {

    public static final String KIND = "PHQ-9";

    public static final List<String> QUESTIONS = List.of(
        "일을 하는 것에 대한 흥미나 재미가 거의 없음",
        "가라앉는 느낌, 우울감 혹은 절망감",
        "잠들기가 어렵거나 자주 깸, 또는 너무 많이 잠",
        "피곤함, 기력이 저하됨",
        "식욕 저하 혹은 과식",
        "자신이 실패자로 느껴지거나 자신과 가족을 실망시켰다고 느낌",
        "신문 읽기나 TV 보기 같은 일상적인 일에도 집중하기 어려움",
        "다른 사람이 알아챌 정도로 거동이나 말이 느림, 또는 너무 안절부절못함",
        "자해나 어떤 식으로든 죽는 것이 더 낫겠다는 생각"
    );

    public static final List<String> OPTION_LABELS = List.of(
        "전혀 없음", "며칠 동안", "일주일 이상", "거의 매일"
    );

    private final AssessmentRepository repo;
    private final JsonStore json;

    public AssessmentService(AssessmentRepository repo, JsonStore json) {
        this.repo = repo;
        this.json = json;
    }

    @Transactional
    public Assessment submit(User patient, Map<String, Integer> answers) {
        int total = answers.values().stream().mapToInt(Integer::intValue).sum();
        Severity sev = Severity.fromScore(total);
        Assessment a = new Assessment();
        a.setPatient(patient);
        a.setHospital(patient.getHospital());
        a.setKind(KIND);
        a.setTotalScore(total);
        a.setSeverity(sev);
        a.setAnswers(json.stringify(answers));
        return repo.save(a);
    }

    public Optional<Assessment> latest(UUID patientId) {
        return repo.findFirstByPatientIdAndKindOrderByCreatedAtDesc(patientId, KIND);
    }

    public List<Assessment> history(UUID patientId) {
        return repo.findByPatientIdAndKindOrderByCreatedAtAsc(patientId, KIND);
    }

    public List<Assessment> latestForPatients(List<UUID> patientIds) {
        // 각 환자별 최신 1건만 — JPA로 group-wise 가져오는 건 복잡하니
        // 전체 후 in-memory에서 환자별 첫 번째 추리기.
        if (patientIds.isEmpty()) return List.of();
        List<Assessment> all = repo.findByPatientIdInAndKindOrderByCreatedAtDesc(patientIds, KIND);
        java.util.Map<UUID, Assessment> first = new java.util.LinkedHashMap<>();
        for (Assessment a : all) {
            first.putIfAbsent(a.getPatient().getId(), a);
        }
        return List.copyOf(first.values());
    }
}
