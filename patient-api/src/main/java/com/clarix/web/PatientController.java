package com.clarix.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.clarix.dto.AssessmentForm;
import com.clarix.dto.DosageFeedbackForm;
import com.clarix.dto.ExerciseLogForm;
import com.clarix.dto.HospitalPickForm;
import com.clarix.dto.MealLogForm;
import com.clarix.dto.MedicationSetupForm;
import com.clarix.dto.MedicationToggleForm;
import com.clarix.dto.MoodEntryForm;
import com.clarix.dto.ShareGrantForm;
import com.clarix.domain.Emotion;
import com.clarix.domain.MedStatus;
import com.clarix.domain.Prescription;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.AssessmentService;
import com.clarix.service.CurrentUser;
import com.clarix.service.DoctorService;
import com.clarix.service.DrugInteractionService;
import com.clarix.service.PatientService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final CurrentUser current;
    private final PatientService patientSvc;
    private final AssessmentService assessmentSvc;
    private final DoctorService doctorSvc;
    private final DrugInteractionService drugSvc;

    public PatientController(CurrentUser current, PatientService patientSvc,
                             AssessmentService assessmentSvc, DoctorService doctorSvc,
                             DrugInteractionService drugSvc) {
        this.current = current;
        this.patientSvc = patientSvc;
        this.assessmentSvc = assessmentSvc;
        this.doctorSvc = doctorSvc;
        this.drugSvc = drugSvc;
    }

    /* ---- Today ---- */

    /** 봉투 strip용 (med, slot) 튜플. */
    public static record DueMedView(Prescription med, String slot) {}

    /** PRN strip용 — 오늘 복용 횟수 + 마지막 복용 시각 + 4h 잠금 여부. */
    public static record PrnView(Prescription rx, int todayCount,
                                  java.time.OffsetDateTime lastTaken, boolean locked) {}

    @GetMapping({"", "/"})
    public String today(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);

        // PHQ-9는 첫 진입 시 강제. 병원 연결은 메인에서 카드로 유도.
        if (assessmentSvc.latest(me.getId()).isEmpty()) return "redirect:/patient/assessment?onboarding=1";

        List<Prescription> rx = patientSvc.activePrescriptions(me.getId());
        // 정규 슬롯만 진행률 분모에 포함 (PRN은 필요 시에만 복용 → 일일 의무가 아님)
        int totalSlots = rx.stream()
            .mapToInt(p -> (int) p.scheduleSlots().stream()
                .filter(s -> !"asneeded".equalsIgnoreCase(s)).count())
            .sum();
        var todayLogs = patientSvc.todayLogs(me.getId());
        // 정규 약 (asneeded 제외)의 TAKEN 수만 진행률에 카운트
        java.util.Set<String> prnNames = rx.stream()
            .filter(p -> p.scheduleSlots().contains("asneeded"))
            .map(Prescription::getMedicationName)
            .collect(java.util.stream.Collectors.toSet());
        long taken = todayLogs.stream()
            .filter(l -> l.getStatus() == MedStatus.TAKEN)
            .filter(l -> !prnNames.contains(l.getMedicationName()))
            .count();

        // 시간대별 상태 인덱스 (today에서 봉투 카드용)
        Map<String, MedStatus> idx = new HashMap<>();
        for (var l : todayLogs) {
            String slot = whichSlot(l.getTakenAt().getHour());
            idx.put(l.getMedicationName() + "|" + slot, l.getStatus());
        }

        // 약별 오늘 마지막 복용 시각 (interval 경고용)
        java.util.Map<String, java.time.OffsetDateTime> lastTakenByMed = new java.util.HashMap<>();
        for (var l : todayLogs) {
            if (l.getStatus() != MedStatus.TAKEN) continue;
            String key = l.getMedicationName();
            var prev = lastTakenByMed.get(key);
            if (prev == null || l.getTakenAt().isAfter(prev)) {
                lastTakenByMed.put(key, l.getTakenAt());
            }
        }

        // 시간대별 처방 그룹화
        java.util.Map<String, java.util.List<Prescription>> rxBySlot = new java.util.LinkedHashMap<>();
        for (String s : new String[] {"morning", "noon", "evening"}) rxBySlot.put(s, new java.util.ArrayList<>());
        for (Prescription p : rx) {
            for (String s : p.scheduleSlots()) {
                var bucket = rxBySlot.get(s);
                if (bucket != null) bucket.add(p);
            }
        }

        int daysToRefill = patientSvc.daysUntilRefill(me.getId());
        boolean isHospitalDay = daysToRefill >= 0 && daysToRefill <= 7;

        // 슬롯별 시간 윈도우 (시작/끝)
        // morning 6-11, noon 11-17, evening 17-24
        int hour = java.time.LocalTime.now().getHour();
        Map<String, String> slotState = new java.util.LinkedHashMap<>();
        Map<String, int[]> slotWindow = Map.of(
            "morning", new int[]{6, 11},
            "noon",    new int[]{11, 17},
            "evening", new int[]{17, 24}
        );
        for (var e : slotWindow.entrySet()) {
            int s = e.getValue()[0], en = e.getValue()[1];
            String state;
            if (hour <  s)  state = "future";
            else if (hour < en) state = "active";
            else                state = "past";
            slotState.put(e.getKey(), state);
        }

        // due-now 알림: 24h 전 구간이 어떤 슬롯에 속하도록 매핑.
        // morning 5-11, noon 11-17, evening 17-24 + 0-5 (overnight). 빈칸 없도록.
        String dueSlot;
        if      (hour >= 5  && hour < 11) dueSlot = "morning";
        else if (hour >= 11 && hour < 17) dueSlot = "noon";
        else                              dueSlot = "evening";
        int dueNow = 0;
        if (dueSlot != null) {
            for (Prescription p : rx) {
                if (p.scheduleSlots().contains(dueSlot)) {
                    String key = p.getMedicationName() + "|" + dueSlot;
                    var st = idx.get(key);
                    if (st == null || st != MedStatus.TAKEN) dueNow++;
                }
            }
        }
        Integer adherence7d = patientSvc.adherence7Days(me.getId());

        // 약 상호작용 위험 검사
        var medicationNames = rx.stream().map(Prescription::getMedicationName).toList();
        var interactionRisks = drugSvc.check(medicationNames);

        String mode;
        if (me.getHospital() == null) mode = "connect";
        else if (isHospitalDay)       mode = "hospital_day";
        else                          mode = "normal";

        model.addAttribute("me", me);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("mode", mode);
        model.addAttribute("rx", rx);
        model.addAttribute("rxBySlot", rxBySlot);
        model.addAttribute("statusIdx", idx);
        model.addAttribute("slotState", slotState);
        model.addAttribute("lastTakenByMed", lastTakenByMed);
        model.addAttribute("hasPrescriptions", !rx.isEmpty());
        model.addAttribute("medTotal", totalSlots);
        model.addAttribute("medTaken", taken);
        model.addAttribute("daysToRefill", daysToRefill);
        model.addAttribute("nextRefillDate", patientSvc.nextRefillDate(me.getId()).orElse(null));
        model.addAttribute("dueSlot", dueSlot);
        model.addAttribute("dueNow", dueNow);
        model.addAttribute("adherence7d", adherence7d);
        model.addAttribute("interactionRisks", interactionRisks);
        var yesterdayMissedRaw = patientSvc.yesterdayMissedSlots(me.getId());
        @SuppressWarnings("unchecked")
        var skippedMedsRaw = (java.util.Set<String>) session.getAttribute("skippedYesterdayMeds");
        final var skippedMeds = skippedMedsRaw == null ? java.util.Set.<String>of() : skippedMedsRaw;
        var yesterdayMissed = yesterdayMissedRaw.stream()
            .filter(m -> !skippedMeds.contains(m.medicationName() + "|" + m.slot()))
            .toList();
        boolean skipMealToday     = session.getAttribute("skip_meal_" + LocalDate.now()) != null;
        boolean skipExerciseToday = session.getAttribute("skip_exercise_" + LocalDate.now()) != null;
        boolean skipMoodToday     = session.getAttribute("skip_mood_" + LocalDate.now()) != null;
        boolean skipDueSlotNow    = dueSlot != null
            && session.getAttribute("skip_slot_" + LocalDate.now() + "_" + dueSlot) != null;

        // 봉투를 방금 찢어 만든 logId만 피드백 노출 (session 기반) — 시드 데이터는 무시
        @SuppressWarnings("unchecked")
        var pendingFeedbackIds = (java.util.Set<UUID>) session.getAttribute("pendingFeedbackLogs");
        java.util.List<com.clarix.domain.MedicationLog> feedbackPending;
        if (pendingFeedbackIds == null || pendingFeedbackIds.isEmpty()) {
            feedbackPending = java.util.List.of();
        } else {
            final var pendingIdsSet = pendingFeedbackIds;
            feedbackPending = patientSvc.todayLogsAwaitingFeedback(me.getId()).stream()
                .filter(l -> pendingIdsSet.contains(l.getId()))
                .toList();
        }
        model.addAttribute("yesterdayMissed", yesterdayMissed);
        model.addAttribute("dosageFeedbackPending", feedbackPending);

        // ============== Wizard nextStep 계산 ==============
        // 우선순위: 어제 미복용 → 지금 시간대 약 → 용량 피드백 → 감정 → 식사 → 운동 → 완료
        String nextStep = "done";
        Prescription stepDueMed = null;
        java.util.List<DueMedView> stepDueMeds = java.util.List.of();
        String stepDueSlot = null;
        com.clarix.domain.MedicationLog stepFeedbackLog = null;
        com.clarix.service.PatientService.MissedSlot stepYesterdayMed = null;

        if (!yesterdayMissed.isEmpty()) {
            nextStep = "yesterday_med";
            stepYesterdayMed = yesterdayMissed.get(0);
        } else if (dueSlot != null && dueNow > 0 && !skipDueSlotNow) {
            // 활성: 현재 슬롯의 첫 미복용 약 (사용자가 지금 뜯을 봉투)
            Prescription active = null;
            for (Prescription p : rxBySlot.get(dueSlot)) {
                String key = p.getMedicationName() + "|" + dueSlot;
                var st = idx.get(key);
                if (st == null || st != MedStatus.TAKEN) { active = p; break; }
            }
            if (active != null) {
                stepDueMed = active;
                stepDueSlot = dueSlot;
                nextStep = "due_med";

                // 모든 슬롯의 미복용 약을 시간 역순으로 큐에 — 활성 = 가운데, 큐 = 왼쪽
                // 표시 순서: [evening][noon][morning-rest][ACTIVE]
                java.util.List<DueMedView> all = new java.util.ArrayList<>();
                all.add(new DueMedView(active, dueSlot));
                for (String s : new String[] {"morning", "noon", "evening"}) {
                    for (Prescription p : rxBySlot.get(s)) {
                        if (s.equals(dueSlot) && p == active) continue;
                        String key = p.getMedicationName() + "|" + s;
                        var st = idx.get(key);
                        if (st == null || st != MedStatus.TAKEN) {
                            all.add(new DueMedView(p, s));
                        }
                    }
                }
                stepDueMeds = all;
            }
        } else if (!feedbackPending.isEmpty()) {
            nextStep = "dosage_feedback";
            stepFeedbackLog = feedbackPending.get(0);
        } else if (mode.equals("normal") && !skipMoodToday
                   && (patientSvc.todayMood(me.getId()).isEmpty()
                       || patientSvc.todayMood(me.getId()).get().getEmotion() == null)) {
            nextStep = "mood";
        } else if (mode.equals("normal") && patientSvc.recentMeals(me.getId(), 1).isEmpty()
                   && !skipMealToday) {
            nextStep = "meal";
        } else if (mode.equals("normal") && patientSvc.recentExercises(me.getId(), 1).isEmpty()
                   && !skipExerciseToday) {
            nextStep = "exercise";
        }

        // 진행률
        int totalTasks = 4; // 약(슬롯 합) / 감정 / 식사 / 운동
        int doneTasks = 0;
        if (totalSlots == 0 || taken >= totalSlots) doneTasks++;
        if (patientSvc.todayMood(me.getId()).isPresent()
            && patientSvc.todayMood(me.getId()).get().getEmotion() != null) doneTasks++;
        if (!patientSvc.recentMeals(me.getId(), 1).isEmpty()) doneTasks++;
        if (!patientSvc.recentExercises(me.getId(), 1).isEmpty()) doneTasks++;

        // done 단계용: 다음 due 슬롯 + 첫 약 (블러 미리보기)
        String nextDueSlot = null;
        java.time.LocalTime nextDueTime = null;
        Prescription nextDueMed = null;
        String[] orderedSlots  = {"morning", "noon", "evening"};
        int[]    slotStartHr   = {6, 11, 17};
        // 오늘 남은 슬롯 우선
        for (int i = 0; i < 3; i++) {
            if (hour < slotStartHr[i] && !rxBySlot.get(orderedSlots[i]).isEmpty()) {
                nextDueSlot = orderedSlots[i];
                nextDueTime = java.time.LocalTime.of(slotStartHr[i], 0);
                nextDueMed  = rxBySlot.get(orderedSlots[i]).get(0);
                break;
            }
        }
        // 오늘 더 없으면 내일 첫 슬롯
        if (nextDueSlot == null) {
            for (int i = 0; i < 3; i++) {
                if (!rxBySlot.get(orderedSlots[i]).isEmpty()) {
                    nextDueSlot = orderedSlots[i];
                    nextDueTime = java.time.LocalTime.of(slotStartHr[i], 0);
                    nextDueMed  = rxBySlot.get(orderedSlots[i]).get(0);
                    break;
                }
            }
        }

        // 필요시 복용 (asneeded) — due_med 단계 위에 별도 strip으로 노출
        java.util.List<Prescription> asNeededRx = rx.stream()
            .filter(p -> p.scheduleSlots().contains("asneeded"))
            .toList();
        // 오늘 PRN 복용 통계 (약 이름별 횟수 + 마지막 시각)
        java.util.Map<String, Integer> prnCountByMed = new java.util.HashMap<>();
        java.util.Map<String, java.time.OffsetDateTime> prnLastByMed = new java.util.HashMap<>();
        for (var l : todayLogs) {
            if (l.getStatus() != MedStatus.TAKEN) continue;
            // 시간대 윈도우 외(11h, 12h 등 디폴트 시각이 아닌 임의 시각) → PRN 로그로 판정
            // → asNeededRx에 같은 약명이 있는지 매칭
            String name = l.getMedicationName();
            boolean isPrnMed = asNeededRx.stream().anyMatch(p -> p.getMedicationName().equals(name));
            if (!isPrnMed) continue;
            prnCountByMed.merge(name, 1, Integer::sum);
            var prev = prnLastByMed.get(name);
            if (prev == null || l.getTakenAt().isAfter(prev)) prnLastByMed.put(name, l.getTakenAt());
        }
        java.util.List<PrnView> prnViews = new java.util.ArrayList<>();
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        for (Prescription p : asNeededRx) {
            var last = prnLastByMed.get(p.getMedicationName());
            int cnt = prnCountByMed.getOrDefault(p.getMedicationName(), 0);
            boolean locked = last != null && java.time.Duration.between(last, now).toHours() < 4;
            prnViews.add(new PrnView(p, cnt, last, locked));
        }
        model.addAttribute("asNeededRx", asNeededRx);
        model.addAttribute("prnViews", prnViews);

        model.addAttribute("nextStep", nextStep);
        model.addAttribute("stepDueMed", stepDueMed);
        model.addAttribute("stepDueMeds", stepDueMeds);
        model.addAttribute("stepDueSlot", stepDueSlot);
        model.addAttribute("stepFeedbackLog", stepFeedbackLog);
        model.addAttribute("stepYesterdayMed", stepYesterdayMed);
        model.addAttribute("nextDueSlot", nextDueSlot);
        model.addAttribute("nextDueTime", nextDueTime);
        model.addAttribute("nextDueMed", nextDueMed);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("doneTasks", doneTasks);
        model.addAttribute("emotions", com.clarix.domain.Emotion.values());
        model.addAttribute("mealKinds", com.clarix.domain.MealKind.values());
        var todayMeals = patientSvc.recentMeals(me.getId(), 1);
        java.util.Set<com.clarix.domain.MealKind> todayMealKindsSet = new java.util.HashSet<>();
        for (var m : todayMeals) todayMealKindsSet.add(m.getKind());
        model.addAttribute("todayMeals", todayMeals);
        model.addAttribute("todayMealKindsSet", todayMealKindsSet);
        model.addAttribute("todayExercises", patientSvc.recentExercises(me.getId(), 1));
        model.addAttribute("mood", patientSvc.todayMood(me.getId()).orElse(null));
        model.addAttribute("phq9", assessmentSvc.latest(me.getId()).orElse(null));
        model.addAttribute("shares", doctorSvc.permissionsForPatient(me.getId()));
        return "patient/today";
    }

    /* ---- Welcome (병원 선택) ---- */

    @GetMapping("/welcome")
    public String welcome(@RequestParam(defaultValue = "partnered") String tab,
                          HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        boolean partnered = !"nearby".equals(tab);
        model.addAttribute("me", me);
        model.addAttribute("hospitals", patientSvc.listHospitals(partnered));
        model.addAttribute("activeTab", partnered ? "partnered" : "nearby");
        return "patient/welcome";
    }

    @PostMapping("/welcome")
    public String pickHospital(@ModelAttribute HospitalPickForm form, HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        patientSvc.setHospital(me, form.getHospitalId());
        return "redirect:/patient/assessment?onboarding=1";
    }

    @PostMapping("/welcome/skip")
    public String skipWelcome(HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        return "redirect:/patient/";
    }

    /* ---- Assessment (PHQ-9) ---- */

    @GetMapping("/assessment")
    public String assessment(@RequestParam(defaultValue = "0") int onboarding,
                             HttpSession session, Model model) {
        current.requireRole(session, Role.PATIENT);
        model.addAttribute("questions", AssessmentService.QUESTIONS);
        model.addAttribute("optionLabels", AssessmentService.OPTION_LABELS);
        model.addAttribute("isOnboarding", onboarding == 1);
        return "patient/assessment";
    }

    @PostMapping("/assessment")
    public String submitAssessment(@ModelAttribute AssessmentForm form, HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        Map<String, Integer> answers;
        try {
            answers = form.toAnswers();
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
        assessmentSvc.submit(me, answers);
        return form.isOnboardingFlow() ? "redirect:/patient/onboarding?onboarding=1"
                                       : "redirect:/patient/assessment/result";
    }

    @GetMapping("/assessment/result")
    public String assessmentResult(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        var latest = assessmentSvc.latest(me.getId()).orElse(null);
        if (latest == null) return "redirect:/patient/assessment";
        var history = assessmentSvc.history(me.getId());
        model.addAttribute("me", me);
        model.addAttribute("phq9", latest);
        model.addAttribute("history", history);
        return "patient/assessment-result";
    }

    /* ---- Onboarding (medication setup) ---- */

    @GetMapping("/onboarding")
    public String onboarding(@RequestParam(defaultValue = "0") int onboarding,
                             HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        model.addAttribute("existing", patientSvc.activePrescriptions(me.getId()));
        model.addAttribute("isOnboarding", onboarding == 1);
        return "patient/onboarding";
    }

    @PostMapping("/onboarding")
    public String addPrescriptions(@ModelAttribute MedicationSetupForm form, HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        for (MedicationSetupForm.Row row : form.rows()) {
            patientSvc.addPrescription(me, row.name(), row.slots(), row.daysSupply(), row.storage());
        }
        return "redirect:/patient/";
    }

    @PostMapping("/onboarding/storage/{id}")
    public String updateStorage(@PathVariable UUID id,
                                @RequestParam(required = false) String storage,
                                HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        patientSvc.updateStorage(id, storage, null);
        return "redirect:/patient/onboarding";
    }

    @PostMapping("/onboarding/deactivate/{id}")
    public String deactivate(@PathVariable UUID id, HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        patientSvc.deactivatePrescription(id);
        return "redirect:/patient/onboarding";
    }

    /* ---- Medication 토글 페이지 ---- */

    @GetMapping("/medication")
    public String medication(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        List<Prescription> rx = patientSvc.activePrescriptions(me.getId());
        var todayLogs = patientSvc.todayLogs(me.getId());

        // map[(name, slot)] = status
        Map<String, MedStatus> idx = new HashMap<>();
        for (var l : todayLogs) {
            String slot = whichSlot(l.getTakenAt().getHour());
            idx.put(l.getMedicationName() + "|" + slot, l.getStatus());
        }
        int totalSlots = rx.stream().mapToInt(p -> p.scheduleSlots().size()).sum();
        long taken = todayLogs.stream().filter(l -> l.getStatus() == MedStatus.TAKEN).count();

        model.addAttribute("rx", rx);
        model.addAttribute("statusIdx", idx);
        model.addAttribute("medTotal", totalSlots);
        model.addAttribute("medTaken", taken);
        model.addAttribute("today", LocalDate.now());
        return "patient/medication";
    }

    @PostMapping("/medication/toggle")
    @SuppressWarnings("unchecked")
    public String toggleMedication(@ModelAttribute MedicationToggleForm form,
                                   HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        var log = patientSvc.logMedication(me, form.getMedicationName(), form.getSlot(),
            MedStatus.valueOf(form.getStatus().toUpperCase()));
        // 봉투를 직접 찢은 케이스만 피드백 단계 노출 — session에 logId 등록
        if (log.getStatus() == MedStatus.TAKEN) {
            var pending = (java.util.Set<UUID>) session.getAttribute("pendingFeedbackLogs");
            if (pending == null) {
                pending = new java.util.HashSet<>();
                session.setAttribute("pendingFeedbackLogs", pending);
            }
            pending.add(log.getId());
        }
        String returnTo = form.getReturnTo();
        return "redirect:" + (returnTo != null && returnTo.startsWith("/") ? returnTo : "/patient/");
    }

    @PostMapping("/medication/feedback")
    @SuppressWarnings("unchecked")
    public String recordFeedback(@ModelAttribute DosageFeedbackForm form,
                                 HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        try {
            patientSvc.recordDosageFeedback(me.getId(), form.getLogId(),
                com.clarix.domain.DosageFeedback.valueOf(form.getFeedback()));
        } catch (IllegalArgumentException ignore) {}
        var pending = (java.util.Set<UUID>) session.getAttribute("pendingFeedbackLogs");
        if (pending != null) pending.remove(form.getLogId());
        return "redirect:/patient/";
    }

    /** 어제 못 챙긴 약을 늦게 기록 — 어제 시각으로 TAKEN 로그. */
    @PostMapping("/medication/log-yesterday")
    public String logYesterday(@ModelAttribute MedicationToggleForm form,
                               HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        patientSvc.logMedicationOn(me, form.getMedicationName(), form.getSlot(),
            MedStatus.TAKEN, LocalDate.now().minusDays(1));
        return "redirect:/patient/";
    }

    @PostMapping("/medication/skip-yesterday")
    @SuppressWarnings("unchecked")
    public String skipYesterdayMed(@ModelAttribute MedicationToggleForm form,
                                   HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        var skipped = (java.util.Set<String>) session.getAttribute("skippedYesterdayMeds");
        if (skipped == null) {
            skipped = new java.util.HashSet<>();
            session.setAttribute("skippedYesterdayMeds", skipped);
        }
        skipped.add(form.getMedicationName() + "|" + form.getSlot());
        return "redirect:/patient/";
    }

    @PostMapping("/meal/skip")
    public String skipMeal(HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        session.setAttribute("skip_meal_" + LocalDate.now(), Boolean.TRUE);
        return "redirect:/patient/";
    }

    @PostMapping("/exercise/skip")
    public String skipExercise(HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        session.setAttribute("skip_exercise_" + LocalDate.now(), Boolean.TRUE);
        return "redirect:/patient/";
    }

    /* ---- 테스트용 스킵 엔드포인트 — 단계별 제한 없이 다음으로 ---- */

    @PostMapping("/medication/skip-slot")
    public String skipDueSlot(@RequestParam String slot, HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        session.setAttribute("skip_slot_" + LocalDate.now() + "_" + slot, Boolean.TRUE);
        return "redirect:/patient/";
    }

    @PostMapping("/medication/skip-feedback")
    public String skipFeedback(HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        session.removeAttribute("pendingFeedbackLogs");
        return "redirect:/patient/";
    }

    @PostMapping("/mood/skip")
    public String skipMood(HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        session.setAttribute("skip_mood_" + LocalDate.now(), Boolean.TRUE);
        return "redirect:/patient/";
    }

    /** 위저드 처음부터: 세션 스킵 + 오늘 모든 로그(약·기분·식사·운동) 삭제. */
    @PostMapping("/test/reset-skips")
    public String resetSkips(HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        java.util.Collections.list(session.getAttributeNames()).stream()
            .filter(n -> n.startsWith("skip_") || n.startsWith("skipped") || n.equals("pendingFeedbackLogs"))
            .forEach(session::removeAttribute);
        patientSvc.resetToday(me.getId());
        return "redirect:/patient/";
    }

    private String whichSlot(int h) {
        if (h < 11) return "morning";
        if (h < 17) return "noon";
        return "evening";
    }

    /* ---- Mood: emotion → journal → preview → save ---- */

    @GetMapping("/mood")
    public String moodPick(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        var existing = patientSvc.todayMood(me.getId()).orElse(null);
        model.addAttribute("me", me);
        model.addAttribute("emotions", Emotion.values());
        model.addAttribute("selected", existing != null ? existing.getEmotion() : null);
        return "patient/mood-pick";
    }

    @PostMapping("/mood/journal")
    public String moodJournal(@ModelAttribute MoodEntryForm form,
                              HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        Emotion e = Emotion.valueOf(form.getEmotion());
        String prefill;
        if (form.getJournal() != null) {
            prefill = form.getJournal();
        } else {
            var existing = patientSvc.todayMood(me.getId()).orElse(null);
            prefill = existing != null && existing.getJournal() != null ? existing.getJournal() : "";
        }
        model.addAttribute("me", me);
        model.addAttribute("emotion", e);
        model.addAttribute("journal", prefill);
        return "patient/mood-journal";
    }

    @PostMapping("/mood/preview")
    public String moodPreview(@ModelAttribute MoodEntryForm form,
                              HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        Emotion e = Emotion.valueOf(form.getEmotion());
        String text = form.getJournal() == null ? "" : form.getJournal().trim();
        if (text.isEmpty()) {
            model.addAttribute("me", me);
            model.addAttribute("emotion", e);
            model.addAttribute("journal", "");
            return "patient/mood-journal";
        }
        model.addAttribute("me", me);
        model.addAttribute("emotion", e);
        model.addAttribute("journal", text);
        return "patient/mood-preview";
    }

    @PostMapping("/mood/save")
    public String moodSave(@ModelAttribute MoodEntryForm form,
                           HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        Emotion e = Emotion.valueOf(form.getEmotion());
        patientSvc.upsertEmotion(me, e, form.getJournal().trim());
        return "redirect:/patient/";
    }

    /* ---- Meal / Exercise quick-log ---- */

    @PostMapping("/meal")
    public String logMeal(@ModelAttribute MealLogForm form,
                          HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        try {
            patientSvc.logMeal(me, com.clarix.domain.MealKind.valueOf(form.getKind()), form.getNote());
        } catch (IllegalArgumentException ignore) {}
        return "redirect:/patient/";
    }

    @PostMapping("/exercise")
    public String logExercise(@ModelAttribute ExerciseLogForm form,
                              HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        if (form.getKind() == null || form.getKind().isBlank()) return "redirect:/patient/";
        patientSvc.logExercise(me, form.getKind(), form.getDurationMin(), form.getNote());
        return "redirect:/patient/";
    }

    /* ---- Sharing (의사 권한) ---- */

    @GetMapping("/sharing")
    public String sharing(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        model.addAttribute("me", me);
        var shares = doctorSvc.permissionsForPatient(me.getId());
        model.addAttribute("shares", shares);
        if (me.getHospital() != null) {
            // 같은 병원 의사 중 아직 권한 부여 안 된 의사만
            var existingIds = shares.stream().map(p -> p.getDoctor().getId()).toList();
            var docs = patientSvc.doctorsAtHospital(me.getHospital().getId()).stream()
                .filter(d -> !existingIds.contains(d.getId()))
                .toList();
            model.addAttribute("hospitalDoctors", docs);
        } else {
            model.addAttribute("hospitalDoctors", java.util.List.of());
        }
        return "patient/sharing";
    }

    @PostMapping("/sharing/grant")
    public String grant(@ModelAttribute ShareGrantForm form, HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        try {
            doctorSvc.grant(me, form.getDoctorId());
        } catch (Exception e) {
            model.addAttribute("error", "권한 부여 실패: " + e.getMessage());
        }
        return "redirect:/patient/sharing";
    }

    @PostMapping("/sharing/{id}/toggle")
    public String toggleShare(@PathVariable UUID id, @RequestParam boolean active, HttpSession session) {
        current.requireRole(session, Role.PATIENT);
        doctorSvc.togglePermission(id, active);
        return "redirect:/patient/sharing";
    }

    /* ---- Calendar ---- */

    public static record DayCell(LocalDate date, int day, int dow, boolean inMonth,
                                  Emotion emotion, String journal,
                                  boolean selected, boolean today) {}

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) String ym,
                           @RequestParam(required = false) String date,
                           HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);

        YearMonth viewMonth;
        try { viewMonth = ym != null ? YearMonth.parse(ym) : YearMonth.now(); }
        catch (DateTimeParseException e) { viewMonth = YearMonth.now(); }

        LocalDate today = LocalDate.now();
        LocalDate selected;
        try {
            selected = date != null ? LocalDate.parse(date)
                : (viewMonth.equals(YearMonth.now()) ? today : viewMonth.atDay(1));
        } catch (DateTimeParseException e) { selected = today; }

        var moods = patientSvc.moodsBetween(me.getId(), viewMonth.atDay(1), viewMonth.atEndOfMonth());
        Map<LocalDate, com.clarix.domain.SymptomLog> moodByDate = new HashMap<>();
        for (var m : moods) moodByDate.put(m.getLogDate(), m);

        // Monday-start 6×7 grid
        LocalDate firstDay = viewMonth.atDay(1);
        int dow = firstDay.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        LocalDate cursor = firstDay.minusDays(dow - 1L);

        var weekField = java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear();
        List<List<DayCell>> weeks = new ArrayList<>();
        List<Integer> weekNumbers = new ArrayList<>();
        for (int w = 0; w < 6; w++) {
            weekNumbers.add(cursor.get(weekField));
            List<DayCell> row = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                var m = moodByDate.get(cursor);
                row.add(new DayCell(
                    cursor,
                    cursor.getDayOfMonth(),
                    cursor.getDayOfWeek().getValue(), // 1=Mon..7=Sun
                    YearMonth.from(cursor).equals(viewMonth),
                    m != null ? m.getEmotion() : null,
                    m != null ? m.getJournal() : null,
                    cursor.equals(selected),
                    cursor.equals(today)
                ));
                cursor = cursor.plusDays(1);
            }
            weeks.add(row);
        }

        var selectedMood = moodByDate.get(selected);
        // selected date might not be in the viewed month — fall back to direct lookup
        if (selectedMood == null && !YearMonth.from(selected).equals(viewMonth)) {
            selectedMood = patientSvc.moodsBetween(me.getId(), selected, selected).stream().findFirst().orElse(null);
        }

        model.addAttribute("me", me);
        model.addAttribute("viewMonth", viewMonth);
        model.addAttribute("prevMonth", viewMonth.minusMonths(1));
        model.addAttribute("nextMonth", viewMonth.plusMonths(1));
        model.addAttribute("weeks", weeks);
        model.addAttribute("weekNumbers", weekNumbers);
        model.addAttribute("selected", selected);
        model.addAttribute("selectedMood", selectedMood);

        // 통합된 인사이트 (이전 /patient/insights와 동일 집계)
        var weekly  = patientSvc.recentMoods(me.getId(), 7);
        var monthly = patientSvc.recentMoods(me.getId(), 30);
        model.addAttribute("topWeek",      topEmotion(weekly));
        model.addAttribute("topMonth",     topEmotion(monthly));
        model.addAttribute("weeklyCount",  weekly.size());
        model.addAttribute("monthlyCount", monthly.size());
        return "patient/calendar";
    }

    @PostMapping("/calendar/delete")
    public String deleteMood(@RequestParam String date, HttpSession session) {
        User me = current.requireRole(session, Role.PATIENT);
        LocalDate d;
        try { d = LocalDate.parse(date); } catch (DateTimeParseException e) { return "redirect:/patient/calendar"; }
        patientSvc.deleteMoodOn(me.getId(), d);
        return "redirect:/patient/calendar?ym=" + YearMonth.from(d) + "&date=" + d;
    }

    /* ---- Insights (merged into calendar) ---- */

    @GetMapping("/insights")
    public String insights() {
        return "redirect:/patient/calendar#insights";
    }

    private static Emotion topEmotion(List<com.clarix.domain.SymptomLog> logs) {
        Map<Emotion, Integer> count = new EnumMap<>(Emotion.class);
        for (var l : logs) {
            if (l.getEmotion() == null) continue;
            count.merge(l.getEmotion(), 1, Integer::sum);
        }
        return count.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /* ---- My page ---- */

    @GetMapping("/me")
    public String myPage(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.PATIENT);
        model.addAttribute("me", me);
        model.addAttribute("phq9", assessmentSvc.latest(me.getId()).orElse(null));
        model.addAttribute("rxCount", patientSvc.activePrescriptions(me.getId()).size());
        return "patient/me";
    }
}
