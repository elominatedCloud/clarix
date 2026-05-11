package com.clarix.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.CurrentUser;
import com.clarix.service.DoctorService;
import com.clarix.service.DrugInteractionService;
import com.clarix.service.GeminiSoapService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/doctor")
public class DoctorApiController {

    private final CurrentUser current;
    private final DoctorService doctorSvc;
    private final DrugInteractionService drugSvc;
    private final GeminiSoapService geminiSvc;

    public DoctorApiController(CurrentUser current,
                               DoctorService doctorSvc,
                               DrugInteractionService drugSvc,
                               GeminiSoapService geminiSvc) {
        this.current = current;
        this.doctorSvc = doctorSvc;
        this.drugSvc = drugSvc;
        this.geminiSvc = geminiSvc;
    }

    /** 차트용 JSON — 작은 fetch로 호출. */
    @GetMapping("/patient/{id}/timeline")
    public Map<String, Object> timeline(@PathVariable UUID id,
                                        @RequestParam(defaultValue = "7") int days,
                                        HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        return doctorSvc.timeline(me.getId(), id, days);
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
    public GeminiSoapService.Soap llmSoap(@PathVariable UUID id,
                                          @RequestParam(name = "freeText", required = false) String freeText,
                                          HttpSession session) {
        current.requireRole(session, Role.DOCTOR);
        return geminiSvc.autofill(id, freeText);
    }
}
