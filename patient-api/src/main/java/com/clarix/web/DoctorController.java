package com.clarix.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.clarix.dto.MemoForm;
import com.clarix.dto.PrescriptionForm;
import com.clarix.dto.SoapNoteForm;
import com.clarix.dto.HospitalPickForm;
import com.clarix.dto.StaffInviteForm;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.CurrentUser;
import com.clarix.service.DoctorService;
import com.clarix.service.DrugInteractionService;
import com.clarix.service.NoteService;
import com.clarix.service.PatientService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final CurrentUser current;
    private final DoctorService doctorSvc;
    private final NoteService noteSvc;
    private final DrugInteractionService drugSvc;
    private final PatientService patientSvc;

    public DoctorController(CurrentUser current, DoctorService doctorSvc,
                            NoteService noteSvc,
                            DrugInteractionService drugSvc,
                            PatientService patientSvc) {
        this.current = current;
        this.doctorSvc = doctorSvc;
        this.noteSvc = noteSvc;
        this.drugSvc = drugSvc;
        this.patientSvc = patientSvc;
    }

    @GetMapping({"", "/"})
    public String index(@RequestParam(defaultValue = "all") String view,
                        @RequestParam(defaultValue = "") String q,
                        HttpSession session, Model model) {
        // MVC Controller는 화면 렌더링을 위한 Model 조립을 담당합니다.
        // 환자별 위험도 계산 자체는 DoctorService.patientsForDoctor가 맡습니다.
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

        String query = q == null ? "" : q.trim();
        String queryLower = query.toLowerCase(java.util.Locale.ROOT);
        var searchedRows = queryLower.isBlank()
            ? allRows
            : allRows.stream()
                .filter(r -> (r.name() != null && r.name().toLowerCase(java.util.Locale.ROOT).contains(queryLower))
                    || r.id().toString().contains(queryLower))
                .toList();

        // 위험 인박스는 PHQ-9 점수가 중등도(MODERATE) 이상인 환자만 노출합니다.
        // 단순 저복약은 환자 목록의 severityTone(warn/danger)으로 충분히 강조되므로
        // 인박스는 정신과적 중증도 위험에 집중하도록 분리합니다.
        var rows = "inbox".equals(view)
            ? searchedRows.stream()
                .filter(r -> r.phq9Severity() != null
                    && r.phq9Severity().ordinal() >= com.clarix.domain.Severity.MODERATE.ordinal())
                .toList()
            : searchedRows;

        model.addAttribute("me", me);
        model.addAttribute("rows", rows);
        model.addAttribute("view", view);
        model.addAttribute("q", query);
        model.addAttribute("totalPatients", allRows.size());
        model.addAttribute("riskCount", riskCount);
        model.addAttribute("warnCount", warnCount);
        model.addAttribute("avgAdherence", avgAdherence);
        model.addAttribute("meanPhq9", meanPhq9);
        model.addAttribute("missingMoodCount", missingMoodCount);
        model.addAttribute("todayBookedPatients", doctorSvc.todayBookedFor(me.getId()));
        return "doctor/index";
    }

    @GetMapping("/patient/{id}")
    public String patientDetail(@PathVariable UUID id,
                                @RequestParam(defaultValue = "chart") String mode,
                                HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        // URL path의 id는 신뢰하지 않고, Service에서 공유 권한을 확인한 뒤 환자를 가져옵니다.
        User patient = doctorSvc.requireSharedPatient(me.getId(), id);
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
        // 환자가 오늘 환자 화면에서 입력한 값을 의사 화면에도 즉시 노출.
        model.addAttribute("todayMood", patientSvc.todayMood(id).orElse(null));
        model.addAttribute("todayMeals", patientSvc.recentMeals(id, 1));
        model.addAttribute("todayExercises", patientSvc.recentExercises(id, 1));
        model.addAttribute("todayExerciseKcal", patientSvc.exerciseKcalToday(id));
        model.addAttribute("todayMealKcal", patientSvc.mealKcalToday(id));
        model.addAttribute("todaySleep", patientSvc.latestSleep(id).orElse(null));
        model.addAttribute("inactiveRx", doctorSvc.inactivePrescriptionsFor(id));
        model.addAttribute("activeMedNamesPipe",
            rxList.stream()
                .map(com.clarix.domain.Prescription::getMedicationName)
                .collect(java.util.stream.Collectors.joining("|")));
        return "doctor/patient";
    }

    @PostMapping("/patient/{id}/soap")
    public String addSoap(@PathVariable UUID id,
                          @Valid @ModelAttribute SoapNoteForm form,
                          BindingResult errors,
                          HttpSession session,
                          RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (errors.hasErrors()) return ValidationFeedback.redirect("/doctor/patient/" + id, errors, redirect);
        doctorSvc.createSoap(me, id, form.getSubjective(), form.getObjective(),
            form.getAssessment(), form.getPlan());
        // SOAP 저장 후 자동으로 처방전 발행 화면으로 이동 (의사가 인쇄/PDF 저장 가능).
        return "redirect:/doctor/patient/" + id + "/prescription-print";
    }

    @PostMapping("/patient/{id}/prescription")
    public String addPrescription(@PathVariable UUID id,
                                  @Valid @ModelAttribute PrescriptionForm form,
                                  BindingResult errors,
                                  HttpSession session,
                                  RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (errors.hasErrors()) {
            return ValidationFeedback.redirect("/doctor/patient/" + id, errors, redirect);
        }
        doctorSvc.addPrescriptionFor(me, id, form.getMedicationName().trim(), form.getSlots(),
            Math.max(1, form.getDaysSupply()));
        return "redirect:/doctor/patient/" + id;
    }

    @PostMapping("/patient/{id}/memo")
    public String updateDoctorMemo(@PathVariable UUID id,
                                   @Valid @ModelAttribute MemoForm form,
                                   BindingResult errors,
                                   HttpSession session,
                                   RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (errors.hasErrors()) return ValidationFeedback.redirect("/doctor/patient/" + id, errors, redirect);
        doctorSvc.updateDoctorMemo(me, id, form.getMemo());
        return "redirect:/doctor/patient/" + id;
    }

    @GetMapping("/patient/{id}/prescription-print")
    public String prescriptionPrint(@PathVariable UUID id, HttpSession session, Model model) {
        User me = current.requireRole(session, Role.DOCTOR);
        User patient = doctorSvc.requireSharedPatient(me.getId(), id);
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
                               @Valid @ModelAttribute SoapNoteForm form,
                               BindingResult errors,
                               HttpSession session,
                               RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (errors.hasErrors()) return ValidationFeedback.redirect("/doctor/note/" + id + "/edit", errors, redirect);
        var n = noteSvc.update(me, id, form.getSubjective(), form.getObjective(),
            form.getAssessment(), form.getPlan());
        return "redirect:/doctor/patient/" + n.getPatient().getId();
    }

    @PostMapping("/note/{id}/delete")
    public String deleteNote(@PathVariable UUID id, HttpSession session) {
        User me = current.requireRole(session, Role.DOCTOR);
        UUID patientId = noteSvc.delete(me, id);
        return "redirect:/doctor/patient/" + patientId;
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
    public String pickHospital(@Valid @ModelAttribute HospitalPickForm form,
                               BindingResult errors,
                               HttpSession session,
                               RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (errors.hasErrors()) return ValidationFeedback.redirect("/doctor/welcome", errors, redirect);
        doctorSvc.assignHospital(me, form.getHospitalId());
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
    public String createInvite(@Valid @ModelAttribute StaffInviteForm form,
                               BindingResult errors,
                               HttpSession session, Model model,
                               RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.DOCTOR);
        if (errors.hasErrors()) return ValidationFeedback.redirect("/doctor/admin", errors, redirect);
        try {
            doctorSvc.createInvite(me, form.getEmail(), form.getRole());
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
