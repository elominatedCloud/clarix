package com.clarix.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.clarix.domain.Role;
import com.clarix.domain.SymptomLog;
import com.clarix.domain.User;
import com.clarix.service.CurrentUser;
import com.clarix.service.DoctorService;
import com.clarix.service.DrugInteractionService;
import com.clarix.service.GeminiSoapService;
import com.clarix.service.PatientService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/doctor")
public class DoctorApiController {

    private final CurrentUser current;
    private final DoctorService doctorSvc;
    private final DrugInteractionService drugSvc;
    private final GeminiSoapService geminiSvc;
    private final PatientService patientSvc;

    public DoctorApiController(CurrentUser current,
                               DoctorService doctorSvc,
                               DrugInteractionService drugSvc,
                               GeminiSoapService geminiSvc,
                               PatientService patientSvc) {
        this.current = current;
        this.doctorSvc = doctorSvc;
        this.drugSvc = drugSvc;
        this.geminiSvc = geminiSvc;
        this.patientSvc = patientSvc;
    }

    /** 차트용 JSON — 작은 fetch로 호출. */
    @GetMapping("/patient/{id}/timeline")
    public Map<String, Object> timeline(@PathVariable UUID id,
                                        @RequestParam(defaultValue = "7") int days,
                                        HttpSession session) {
        // @RestController는 템플릿 이름이 아니라 객체를 JSON으로 직렬화해 응답합니다.
        User me = current.requireRole(session, Role.DOCTOR);
        return doctorSvc.timeline(me.getId(), id, days);
    }

    /** 환자 기분 변경 감지용 경량 상태값. 열린 의사 상세 화면이 새 기록을 자동 반영할 때 사용합니다. */
    @GetMapping("/patient/{id}/mood-state")
    public Map<String, Object> moodState(@PathVariable UUID id, HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        doctorSvc.requireSharedPatient(me.getId(), id);

        SymptomLog mood = patientSvc.todayMood(id).orElse(null);
        Map<String, Object> out = new HashMap<>();
        out.put("signature", moodSignature(mood));
        if (mood != null && mood.getEmotion() != null) {
            out.put("date", mood.getLogDate().toString());
            out.put("emotion", mood.getEmotion().name());
            out.put("label", mood.getEmotion().label());
            out.put("journal", mood.getJournal() == null ? "" : mood.getJournal());
        }
        return out;
    }

    private static String moodSignature(SymptomLog mood) {
        if (mood == null || mood.getEmotion() == null) return "none";
        String journal = mood.getJournal() == null ? "" : mood.getJournal();
        return mood.getLogDate() + "|" + mood.getEmotion().name() + "|" + journal;
    }

    /** 약물 상호작용 사전 체크 — 처방 추가 직전 폼이 호출. */
    @PostMapping("/drug-check")
    public List<DrugInteractionService.Risk> drugCheck(
            @RequestParam(name = "med", required = false) List<String> meds,
            HttpSession session) {
        current.requireRole(session, Role.DOCTOR);
        if (meds == null) return List.of();
        return drugSvc.check(meds);
    }

    /** LLM SOAP autofill — 자유 텍스트 + 환자 컨텍스트 -> JSON {S, O, A, P}. */
    @PostMapping("/patient/{id}/llm-soap")
    public ResponseEntity<?> llmSoap(@PathVariable UUID id,
                                     @RequestParam(name = "freeText", required = false) String freeText,
                                     HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        try {
            return ResponseEntity.ok(geminiSvc.autofill(me.getId(), id, freeText));
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 503) {
                return ResponseEntity.status(503).body(Map.of(
                    "error", "AI_CONFIG_MISSING",
                    "message", ex.getReason() == null ? "AI API key is not configured" : ex.getReason()
                ));
            }
            throw ex;
        }
    }
}
