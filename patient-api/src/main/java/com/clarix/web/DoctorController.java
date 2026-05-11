package com.clarix.web;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.UserRepository;
import com.clarix.service.CurrentUser;
import com.clarix.service.DoctorService;
import com.clarix.service.DrugInteractionService;
import com.clarix.service.GeminiSoapService;
import com.clarix.service.NoteService;
import com.clarix.service.PatientService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final CurrentUser current;
    private final DoctorService doctorSvc;
    private final NoteService noteSvc;
    private final DrugInteractionService drugSvc;
    private final PatientService patientSvc;
    private final GeminiSoapService geminiSvc;
    private final UserRepository users;

    public DoctorController(CurrentUser current, DoctorService doctorSvc,
                            NoteService noteSvc, DrugInteractionService drugSvc,
                            PatientService patientSvc, GeminiSoapService geminiSvc,
                            UserRepository users) {
        this.current = current;
        this.doctorSvc = doctorSvc;
        this.noteSvc = noteSvc;
        this.drugSvc = drugSvc;
        this.patientSvc = patientSvc;
        this.geminiSvc = geminiSvc;
        this.users = users;
    }

    @GetMapping({"", "/"})
    public String index(@RequestParam(defaultValue = "all") String view,
                        HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (me.getHospital() == null) return "redirect:/doctor/welcome";
        var allRows = doctorSvc.patientsForDoctor(me.getId(), 7);
        long riskCount = allRows.stream().filter(r -> "danger".equals(r.severityTone())).count();
        long warnCount = allRows.stream().filter(r -> "warn".equals(r.severityTone())).count();

        var adherences = allRows.stream()
            .filter(r -> r.adherence() != null)
            .mapToInt(com.clarix.service.DoctorService.PatientRow::adherence).toArray();
        Integer avgAdherence = adherences.length == 0 ? null
            : (int) Math.round(java.util.Arrays.stream(adherences).average().orElse(0));

        var phq9Scores = allRows.stream()
            .filter(r -> r.phq9Score() != null)
            .mapToInt(com.clarix.service.DoctorService.PatientRow::phq9Score).toArray();
        Integer meanPhq9 = phq9Scores.length == 0 ? null
            : (int) Math.round(java.util.Arrays.stream(phq9Scores).average().orElse(0));

        long missingMoodCount = allRows.stream()
            .filter(r -> r.lastEmotion() == null).count();

        var rows = "inbox".equals(view)
            ? allRows.stream().filter(r -> "danger".equals(r.severityTone())).toList()
            : allRows;

        model.addAttribute("me", me);
        model.addAttribute("rows", rows);
        model.addAttribute("view", view);
        model.addAttribute("totalPatients", allRows.size());
        model.addAttribute("riskCount", riskCount);
        model.addAttribute("warnCount", warnCount);
        model.addAttribute("avgAdherence", avgAdherence);
        model.addAttribute("meanPhq9", meanPhq9);
        model.addAttribute("missingMoodCount", missingMoodCount);
        return "doctor/index";
    }

    @GetMapping("/patient/{id}")
    public String patientDetail(@PathVariable UUID id,
                                @RequestParam(defaultValue = "chart") String mode,
                                HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        User patient = users.findById(id).orElseThrow();
        // permission check via timeline()
        doctorSvc.timeline(me.getId(), id, 7);
        model.addAttribute("me", me);
        model.addAttribute("patient", patient);
        model.addAttribute("mode", mode);
        model.addAttribute("header", doctorSvc.patientHeader(id));
        model.addAttribute("notes", doctorSvc.notesForPatient(id));
        var rxList = doctorSvc.prescriptionsForPatient(id);
        model.addAttribute("rx", rxList);
        model.addAttribute("phq9History", doctorSvc.phq9HistoryFor(id));
        model.addAttribute("timeline", doctorSvc.timelineEvents(id, 20));
        model.addAttribute("interactionRisks",
            drugSvc.check(rxList.stream()
                .map(com.clarix.domain.Prescription::getMedicationName).toList()));
        var spark = doctorSvc.exerciseMinutesPerDay(id, 7);
        int totalEx = spark.stream().mapToInt(com.clarix.service.DoctorService.DaySpark::minutes).sum();
        int maxEx   = spark.stream().mapToInt(com.clarix.service.DoctorService.DaySpark::minutes).max().orElse(0);
        model.addAttribute("exerciseSpark", spark);
        model.addAttribute("exerciseTotalMin", totalEx);
        model.addAttribute("exerciseMaxMin", maxEx);

        // 처방별 누적 복약 (Map<rxId, RxAdherence>)
        java.util.Map<java.util.UUID, com.clarix.service.PatientService.RxAdherence> rxAdherence = new java.util.HashMap<>();
        for (var rx : rxList) {
            rxAdherence.put(rx.getId(), patientSvc.cumulativeAdherence(id, rx));
        }
        model.addAttribute("rxAdherence", rxAdherence);
        model.addAttribute("averages", doctorSvc.patientAverages(id));
        model.addAttribute("inactiveRx", doctorSvc.inactivePrescriptionsFor(id));
        model.addAttribute("activeMedNamesPipe",
            rxList.stream()
                .map(com.clarix.domain.Prescription::getMedicationName)
                .collect(java.util.stream.Collectors.joining("|")));
        return "doctor/patient";
    }

    /** 차트용 JSON — 작은 fetch로 호출 (HTML 안 만큼 사용). */
    @GetMapping("/patient/{id}/timeline")
    @ResponseBody
    public Map<String, Object> timeline(@PathVariable UUID id,
                                        @RequestParam(defaultValue = "7") int days,
                                        HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        return doctorSvc.timeline(me.getId(), id, days);
    }

    /** 약물 상호작용 사전 체크 — 처방 추가 직전 폼이 호출. */
    @PostMapping("/drug-check")
    @ResponseBody
    public java.util.List<DrugInteractionService.Risk> drugCheck(
            @RequestParam(name = "med", required = false) java.util.List<String> meds,
            HttpSession session) {
        current.requireRole(session, Role.DOCTOR);
        if (meds == null) return java.util.List.of();
        return drugSvc.check(meds);
    }

    /** LLM SOAP autofill — 자유 텍스트 + 환자 컨텍스트 → JSON {S, O, A, P} */
    @PostMapping("/patient/{id}/llm-soap")
    @ResponseBody
    public GeminiSoapService.Soap llmSoap(@PathVariable UUID id,
                                          @RequestParam(name = "freeText", required = false) String freeText,
                                          HttpSession session) {
        current.requireRole(session, Role.DOCTOR);
        return geminiSvc.autofill(id, freeText);
    }

    @PostMapping("/patient/{id}/soap")
    public String addSoap(@PathVariable UUID id,
                          @RequestParam(required = false) String subjective,
                          @RequestParam(required = false) String objective,
                          @RequestParam(required = false) String assessment,
                          @RequestParam(required = false) String plan,
                          HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        doctorSvc.createSoap(me, id, subjective, objective, assessment, plan);
        return "redirect:/doctor/patient/" + id;
    }

    @PostMapping("/patient/{id}/prescription")
    public String addPrescription(@PathVariable UUID id,
                                  @RequestParam String medicationName,
                                  @RequestParam(name = "slots", required = false) java.util.List<String> slots,
                                  @RequestParam(defaultValue = "30") int daysSupply,
                                  HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (medicationName == null || medicationName.isBlank() || slots == null || slots.isEmpty()) {
            return "redirect:/doctor/patient/" + id;
        }
        doctorSvc.addPrescriptionFor(me, id, medicationName.trim(), slots,
            Math.max(1, daysSupply));
        return "redirect:/doctor/patient/" + id;
    }

    @PostMapping("/patient/{id}/memo")
    public String updateDoctorMemo(@PathVariable UUID id,
                                   @RequestParam(required = false) String memo,
                                   HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        doctorSvc.updateDoctorMemo(me, id, memo);
        return "redirect:/doctor/patient/" + id;
    }

    @GetMapping("/patient/{id}/prescription-print")
    public String prescriptionPrint(@PathVariable UUID id, HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        User patient = users.findById(id).orElseThrow();
        // 권한 체크는 doctorSvc.timeline()이 보장 (lastViewedAt 갱신 부수효과 없이는 별도 호출)
        // 단순화: timeline() 호출로 권한 검증
        doctorSvc.timeline(me.getId(), id, 1);
        model.addAttribute("me", me);
        model.addAttribute("patient", patient);
        model.addAttribute("rx", doctorSvc.prescriptionsForPatient(id));
        model.addAttribute("issuedAt", java.time.LocalDate.now());
        return "doctor/prescription-print";
    }

    @PostMapping("/prescription/{rxId}/deactivate")
    public String deactivatePrescription(@PathVariable UUID rxId, HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        UUID patientId = doctorSvc.deactivatePrescriptionFor(me, rxId);
        return "redirect:/doctor/patient/" + patientId;
    }

    /* ---- Note edit (author only) ---- */

    @GetMapping("/note/{id}/edit")
    public String editNote(@PathVariable UUID id, HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        var note = noteSvc.requireOwn(me, id);
        model.addAttribute("me", me);
        model.addAttribute("note", note);
        model.addAttribute("backUrl", "/doctor/patient/" + note.getPatient().getId());
        return "note-edit";
    }

    @PostMapping("/note/{id}/edit")
    public String saveNoteEdit(@PathVariable UUID id,
                               @RequestParam(required = false) String subjective,
                               @RequestParam(required = false) String objective,
                               @RequestParam(required = false) String assessment,
                               @RequestParam(required = false) String plan,
                               HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        var n = noteSvc.update(me, id, subjective, objective, assessment, plan);
        return "redirect:/doctor/patient/" + n.getPatient().getId();
    }

    /* ---- Admin: staff invite 관리 ---- */

    /* ---- Doctor welcome (병원 자가 설정) ---- */

    @GetMapping("/welcome")
    public String welcome(@RequestParam(defaultValue = "partnered") String tab,
                          HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        boolean partnered = !"nearby".equals(tab);
        var hospitals = doctorSvc.hospitalsByPartnered(partnered);
        model.addAttribute("me", me);
        model.addAttribute("hospitals", hospitals);
        model.addAttribute("activeTab", partnered ? "partnered" : "nearby");
        return "doctor/welcome";
    }

    @PostMapping("/welcome")
    public String pickHospital(@RequestParam UUID hospitalId, HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        doctorSvc.assignHospital(me, hospitalId);
        return "redirect:/doctor/";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        model.addAttribute("me", me);
        model.addAttribute("invites", doctorSvc.invitesForDoctor(me.getId()));
        model.addAttribute("staff", doctorSvc.staffOfDoctor(me));
        model.addAttribute("staffRoles", java.util.List.of(
            Role.NURSE, Role.TECHNICIAN, Role.RECEPTIONIST));
        return "doctor/admin";
    }

    @PostMapping("/admin/invite")
    public String createInvite(@RequestParam String email,
                               @RequestParam String role,
                               HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        try {
            doctorSvc.createInvite(me, email, Role.valueOf(role));
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            // surfaced via flash if needed; for now just redirect
        }
        return "redirect:/doctor/admin";
    }

    @PostMapping("/admin/invite/{id}/toggle")
    public String toggleInvite(@PathVariable UUID id,
                               @RequestParam boolean active,
                               HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        doctorSvc.toggleInvite(me, id, active);
        return "redirect:/doctor/admin";
    }
}
