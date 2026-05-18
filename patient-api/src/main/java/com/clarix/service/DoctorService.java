package com.clarix.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Assessment;
import com.clarix.domain.ClinicalNote;
import com.clarix.domain.Emotion;
import com.clarix.domain.MedicationLog;
import com.clarix.domain.Permission;
import com.clarix.domain.Prescription;
import com.clarix.domain.Severity;
import com.clarix.domain.SymptomLog;
import com.clarix.domain.User;
import com.clarix.domain.StaffInvite;
import com.clarix.domain.Hospital;
import com.clarix.repo.ClinicalNoteRepository;
import com.clarix.repo.ExerciseLogRepository;
import com.clarix.repo.HospitalRepository;
import com.clarix.repo.MealLogRepository;
import com.clarix.repo.MedicationLogRepository;
import com.clarix.repo.PermissionRepository;
import com.clarix.repo.PrescriptionRepository;
import com.clarix.repo.StaffInviteRepository;
import com.clarix.repo.SymptomLogRepository;
import com.clarix.repo.UserRepository;

@Service
public class DoctorService {

    private final PermissionRepository permissions;
    private final PrescriptionRepository prescriptions;
    private final MedicationLogRepository medLogs;
    private final SymptomLogRepository symptomLogs;
    private final ClinicalNoteRepository notes;
    private final AssessmentService assessments;
    private final UserRepository users;
    private final StaffInviteRepository invites;
    private final HospitalRepository hospitals;
    private final MealLogRepository mealLogs;
    private final ExerciseLogRepository exerciseLogs;

    public DoctorService(PermissionRepository permissions, PrescriptionRepository prescriptions,
                         MedicationLogRepository medLogs, SymptomLogRepository symptomLogs,
                         ClinicalNoteRepository notes, AssessmentService assessments,
                         UserRepository users, StaffInviteRepository invites,
                         HospitalRepository hospitals,
                         MealLogRepository mealLogs, ExerciseLogRepository exerciseLogs) {
        this.permissions = permissions;
        this.prescriptions = prescriptions;
        this.medLogs = medLogs;
        this.symptomLogs = symptomLogs;
        this.notes = notes;
        this.assessments = assessments;
        this.users = users;
        this.invites = invites;
        this.hospitals = hospitals;
        this.mealLogs = mealLogs;
        this.exerciseLogs = exerciseLogs;
    }

    public List<Hospital> hospitalsByPartnered(boolean partnered) {
        return hospitals.findByPartneredOrderByDistanceKmAsc(partnered);
    }

    public User requireSharedPatient(UUID doctorId, UUID patientId) {
        return permissions.findByPatientIdAndDoctorId(patientId, doctorId)
            .filter(Permission::isActive)
            .map(Permission::getPatient)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission"));
    }

    @Transactional
    public void assignHospital(User doctor, java.util.UUID hospitalId) {
        Hospital h = hospitals.findById(hospitalId).orElseThrow();
        doctor.setHospital(h);
        users.save(doctor);
    }

    public record PatientRow(
        UUID id, String name,
        Integer adherence, Double avgMood,
        OffsetDateTime lastLog, OffsetDateTime lastSoap,
        Integer phq9Score, Severity phq9Severity, OffsetDateTime phq9At,
        Emotion lastEmotion, String lastJournal
    ) {
        public String severityTone() {
            if (adherence != null && adherence < 50) return "danger";
            if (phq9Severity == Severity.SEVERE || phq9Severity == Severity.MODERATELY_SEVERE) return "danger";
            if (adherence != null && adherence < 80) return "warn";
            if (phq9Severity == Severity.MODERATE) return "warn";
            if (adherence == null && phq9Score == null) return "muted";
            return "ok";
        }
        public String severityLabel() {
            return switch (severityTone()) {
                case "danger" -> "위험";
                case "warn"   -> "주의";
                case "ok"     -> "안정";
                default       -> "대기";
            };
        }
    }

    public List<PatientRow> patientsForDoctor(UUID doctorId, int days) {
        List<Permission> perms = permissions.findByDoctorIdAndActiveTrue(doctorId);
        if (perms.isEmpty()) return List.of();
        List<UUID> patientIds = perms.stream().map(p -> p.getPatient().getId()).toList();
        Map<UUID, String> nameById = new HashMap<>();
        for (Permission p : perms) nameById.put(p.getPatient().getId(), p.getPatient().getName());

        OffsetDateTime since = LocalDate.now().minusDays(days - 1L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        // 환자별 처방 슬롯 합
        Map<UUID, Integer> slotsByPatient = new HashMap<>();
        for (UUID pid : patientIds) {
            int total = prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(pid)
                .stream().mapToInt(p -> p.scheduleSlots().size()).sum();
            slotsByPatient.put(pid, total);
        }

        List<MedicationLog> logs = medLogs.findByPatientIdInAndTakenAtGreaterThanEqual(patientIds, since);
        Map<UUID, Integer> takenByPatient = new HashMap<>();
        Map<UUID, OffsetDateTime> lastLogByPatient = new HashMap<>();
        for (MedicationLog l : logs) {
            UUID pid = l.getPatient().getId();
            if (l.getStatus() == com.clarix.domain.MedStatus.TAKEN) takenByPatient.merge(pid, 1, Integer::sum);
            lastLogByPatient.merge(pid, l.getTakenAt(), (a, b) -> a.isAfter(b) ? a : b);
        }

        List<SymptomLog> moods = symptomLogs.findByPatientIdInAndLogDateGreaterThanEqual(
            patientIds, LocalDate.now().minusDays(days - 1L));
        Map<UUID, List<Integer>> moodAcc = new HashMap<>();
        Map<UUID, SymptomLog> lastMoodByPatient = new HashMap<>();
        for (SymptomLog s : moods) {
            UUID pid = s.getPatient().getId();
            if (s.getMoodScore() != null) {
                moodAcc.computeIfAbsent(pid, k -> new ArrayList<>()).add(s.getMoodScore());
            }
            var prev = lastMoodByPatient.get(pid);
            if (prev == null || s.getLogDate().isAfter(prev.getLogDate())) {
                lastMoodByPatient.put(pid, s);
            }
        }

        List<ClinicalNote> recentNotes = notes
            .findByDoctorIdAndPatientIdInOrderByCreatedAtDesc(doctorId, patientIds);
        Map<UUID, OffsetDateTime> lastSoapByPatient = new HashMap<>();
        for (ClinicalNote n : recentNotes) {
            lastSoapByPatient.putIfAbsent(n.getPatient().getId(), n.getCreatedAt());
        }

        List<Assessment> phq9List = assessments.latestForPatients(patientIds);
        Map<UUID, Assessment> phq9ByPatient = new HashMap<>();
        for (Assessment a : phq9List) phq9ByPatient.put(a.getPatient().getId(), a);

        List<PatientRow> rows = new ArrayList<>();
        for (UUID pid : patientIds) {
            int expected = slotsByPatient.getOrDefault(pid, 0) * days;
            Integer adherence = expected == 0 ? null
                : (int) Math.round(takenByPatient.getOrDefault(pid, 0) * 100.0 / expected);
            List<Integer> ms = moodAcc.getOrDefault(pid, List.of());
            Double avgMood = ms.isEmpty() ? null
                : Math.round(ms.stream().mapToInt(Integer::intValue).average().orElse(0) * 10.0) / 10.0;
            Assessment phq = phq9ByPatient.get(pid);
            SymptomLog lastMood = lastMoodByPatient.get(pid);
            rows.add(new PatientRow(
                pid, nameById.get(pid),
                adherence, avgMood,
                lastLogByPatient.get(pid),
                lastSoapByPatient.get(pid),
                phq == null ? null : phq.getTotalScore(),
                phq == null ? null : phq.getSeverity(),
                phq == null ? null : phq.getCreatedAt(),
                lastMood == null ? null : lastMood.getEmotion(),
                lastMood == null ? null : lastMood.getJournal()
            ));
        }
        // 위험 우선
        rows.sort((a, b) -> {
            int aR = "danger".equals(a.severityTone()) ? 0 : 1;
            int bR = "danger".equals(b.severityTone()) ? 0 : 1;
            if (aR != bR) return Integer.compare(aR, bR);
            int av = a.adherence == null ? 999 : a.adherence;
            int bv = b.adherence == null ? 999 : b.adherence;
            return Integer.compare(av, bv);
        });
        return rows;
    }

    /** Chart.js datasets에 맞춘 일자별 시리즈. 권한 체크 + lastViewedAt 갱신. */
    @Transactional
    public Map<String, Object> timeline(UUID doctorId, UUID patientId, int days) {
        var perm = permissions.findByPatientIdAndDoctorId(patientId, doctorId)
            .filter(Permission::isActive)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission"));
        perm.setLastViewedAt(OffsetDateTime.now());
        permissions.save(perm);

        LocalDate sinceDate = LocalDate.now().minusDays(days - 1L);
        List<String> labels = new ArrayList<>();
        Map<String, Integer> takenByDay   = new LinkedHashMap<>();
        Map<String, Integer> missedByDay  = new LinkedHashMap<>();
        Map<String, Integer> exMinByDay   = new LinkedHashMap<>();
        Map<String, Integer> mealByDay    = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            String d = sinceDate.plusDays(i).toString();
            labels.add(d);
            takenByDay.put(d, 0);
            missedByDay.put(d, 0);
            exMinByDay.put(d, 0);
            mealByDay.put(d, 0);
        }

        // 일별 expected = 활성 처방의 슬롯 합 (단순화 — 처방 시작·종료일 무시)
        int expectedPerDay = prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId)
            .stream().mapToInt(p -> p.scheduleSlots().size()).sum();

        OffsetDateTime since = sinceDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        for (MedicationLog l : medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since)) {
            String d = l.getTakenAt().toLocalDate().toString();
            if (!takenByDay.containsKey(d)) continue;
            if (l.getStatus() == com.clarix.domain.MedStatus.TAKEN) takenByDay.merge(d, 1, Integer::sum);
            else                                                     missedByDay.merge(d, 1, Integer::sum);
        }

        Map<String, Integer> moodMap = new HashMap<>();
        for (SymptomLog s : symptomLogs.findByPatientIdAndLogDateGreaterThanEqualOrderByLogDateAsc(patientId, sinceDate)) {
            String d = s.getLogDate().toString();
            if (s.getMoodScore() != null) moodMap.put(d, s.getMoodScore());
        }

        for (var ex : exerciseLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since)) {
            String d = ex.getLoggedAt().toLocalDate().toString();
            if (exMinByDay.containsKey(d)) exMinByDay.merge(d, ex.getDurationMin(), Integer::sum);
        }
        for (var ml : mealLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since)) {
            String d = ml.getLoggedAt().toLocalDate().toString();
            if (mealByDay.containsKey(d)) mealByDay.merge(d, 1, Integer::sum);
        }

        // PHQ-9 history (모든 검사) — 별도 시계열
        List<Map<String, Object>> phq9 = new ArrayList<>();
        for (var a : assessments.history(patientId)) {
            var row = new LinkedHashMap<String, Object>();
            row.put("date",  a.getCreatedAt().toLocalDate().toString());
            row.put("score", a.getTotalScore());
            row.put("severity", a.getSeverity().name());
            phq9.add(row);
        }

        // 처방 lifecycle (시작 ~ 종료) — episode timeline용
        List<Map<String, Object>> prescriptionLifecycles = new ArrayList<>();
        for (var p : prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId)) {
            var row = new LinkedHashMap<String, Object>();
            row.put("name",      p.getMedicationName());
            row.put("startDate", p.getCreatedAt().toLocalDate().toString());
            row.put("endDate",   p.endDate().toString());
            row.put("slots",     p.scheduleSlots());
            prescriptionLifecycles.add(row);
        }

        // SOAP/노트 일자 (episode timeline용)
        List<String> noteDates = new ArrayList<>();
        for (var n : notes.findByPatientIdOrderByCreatedAtDesc(patientId)) {
            noteDates.add(n.getCreatedAt().toLocalDate().toString());
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("labels", labels);
        resp.put("medicationTaken",  labels.stream().map(takenByDay::get).toList());
        resp.put("medicationMissed", labels.stream().map(missedByDay::get).toList());
        resp.put("expectedPerDay",   expectedPerDay);
        List<Object> moodList = new ArrayList<>();
        for (String d : labels) moodList.add(moodMap.getOrDefault(d, null));
        resp.put("mood", moodList);
        resp.put("exerciseMin", labels.stream().map(exMinByDay::get).toList());
        resp.put("mealCount",   labels.stream().map(mealByDay::get).toList());
        resp.put("phq9",        phq9);
        resp.put("prescriptionLifecycles", prescriptionLifecycles);
        resp.put("noteDates",    noteDates);
        return resp;
    }

    @Transactional
    public ClinicalNote createSoap(User doctor, UUID patientId, String s, String o, String a, String p) {
        User patient = users.findById(patientId).orElseThrow();
        if (!permissions.existsByPatientIdAndDoctorIdAndActiveTrue(patientId, doctor.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        ClinicalNote n = new ClinicalNote();
        n.setDoctor(doctor);
        n.setPatient(patient);
        n.setSubjective(s); n.setObjective(o); n.setAssessment(a); n.setPlan(p);
        n.setKind(com.clarix.domain.NoteKind.forRole(doctor.getRole()));
        return notes.save(n);
    }

    public List<ClinicalNote> notesForPatient(UUID patientId) {
        return notes.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /** 환자가 의사에게 권한 부여. */
    @Transactional
    public Permission grant(User patient, UUID doctorId) {
        User doctor = users.findById(doctorId).orElseThrow();
        Permission existing = permissions.findByPatientIdAndDoctorId(patient.getId(), doctorId).orElse(null);
        if (existing != null) {
            existing.setActive(true);
            return permissions.save(existing);
        }
        Permission perm = new Permission();
        perm.setPatient(patient);
        perm.setDoctor(doctor);
        perm.setActive(true);
        return permissions.save(perm);
    }

    @Transactional
    public void togglePermission(UUID permissionId, boolean active) {
        permissions.findById(permissionId).ifPresent(p -> { p.setActive(active); permissions.save(p); });
    }

    public List<Permission> permissionsForPatient(UUID patientId) {
        return permissions.findByPatientId(patientId);
    }

    public List<Prescription> prescriptionsForPatient(UUID patientId) {
        return prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId);
    }

    public List<Prescription> inactivePrescriptionsFor(UUID patientId) {
        return prescriptions.findByPatientIdAndActiveFalseOrderByCreatedAtDesc(patientId);
    }

    @Transactional
    public Prescription addPrescriptionFor(User doctor, UUID patientId, String name,
                                           List<String> slots, int daysSupply) {
        if (!permissions.existsByPatientIdAndDoctorIdAndActiveTrue(patientId, doctor.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        User patient = users.findById(patientId).orElseThrow();
        Prescription p = new Prescription();
        p.setPatient(patient);
        p.setMedicationName(name);
        p.setScheduleSlots(slots);
        p.setDaysSupply(daysSupply);
        p.setActive(true);
        return prescriptions.save(p);
    }

    /* ---- Staff invites (admin) ---- */

    public List<StaffInvite> invitesForDoctor(UUID doctorId) {
        return invites.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    /** 의사 본인 hospital의 staff(가입 완료된 user) 목록. */
    public List<User> staffOfDoctor(User doctor) {
        if (doctor.getHospital() == null) return List.of();
        return users.findByHospitalIdAndRoleIn(
            doctor.getHospital().getId(),
            List.of(com.clarix.domain.Role.NURSE,
                    com.clarix.domain.Role.TECHNICIAN,
                    com.clarix.domain.Role.RECEPTIONIST));
    }

    @Transactional
    public StaffInvite createInvite(User doctor, String email, com.clarix.domain.Role role) {
        if (doctor.getHospital() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "병원이 설정되지 않았습니다");
        }
        if (!role.isStaffInvitable()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "초대할 수 없는 역할");
        }
        StaffInvite inv = new StaffInvite();
        inv.setEmail(email.trim().toLowerCase());
        inv.setRole(role);
        inv.setHospital(doctor.getHospital());
        inv.setDoctor(doctor);
        inv.setActive(true);
        return invites.save(inv);
    }

    @Transactional
    public void toggleInvite(User doctor, UUID inviteId, boolean active) {
        StaffInvite inv = invites.findById(inviteId).orElseThrow();
        if (!inv.getDoctor().getId().equals(doctor.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        inv.setActive(active);
        invites.save(inv);
    }

    @Transactional
    public void updateDoctorMemo(User doctor, UUID patientId, String memo) {
        if (!permissions.existsByPatientIdAndDoctorIdAndActiveTrue(patientId, doctor.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        User patient = users.findById(patientId).orElseThrow();
        patient.setDoctorMemo(memo == null || memo.isBlank() ? null : memo.trim());
        users.save(patient);
    }

    @Transactional
    public UUID deactivatePrescriptionFor(User doctor, UUID prescriptionId) {
        Prescription p = prescriptions.findById(prescriptionId).orElseThrow();
        UUID patientId = p.getPatient().getId();
        if (!permissions.existsByPatientIdAndDoctorIdAndActiveTrue(patientId, doctor.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        p.setActive(false);
        prescriptions.save(p);
        return patientId;
    }

    public List<Assessment> phq9HistoryFor(UUID patientId) {
        return assessments.history(patientId);
    }

    /** 환자 일 평균치 (현재 처방 사이클 기준). 활성 처방 없으면 14일 fallback. */
    public record PatientAverages(
        int windowDays,
        LocalDate windowStart,
        boolean hasActiveRx,
        double mealsPerDay,
        double medsPerDay,
        double exerciseMinPerDay,
        Double avgMood
    ) {}

    public PatientAverages patientAverages(UUID patientId) {
        // 가장 최근에 시작된 활성 처방의 시작일을 사이클 시작으로
        var rxList = prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId);
        LocalDate today = LocalDate.now();
        LocalDate sinceDate;
        int days;
        boolean hasRx = !rxList.isEmpty();
        if (hasRx) {
            // 가장 늦게 추가된 처방
            var latest = rxList.get(rxList.size() - 1);
            sinceDate = latest.getCreatedAt().toLocalDate();
            long elapsed = java.time.temporal.ChronoUnit.DAYS.between(sinceDate, today) + 1;
            days = (int) Math.max(1, Math.min(elapsed, latest.getDaysSupply()));
        } else {
            days = 14;
            sinceDate = today.minusDays(days - 1L);
        }

        OffsetDateTime since = sinceDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        int meals = mealLogs
            .findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since).size();
        long meds = medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since).stream()
            .filter(l -> l.getStatus() == com.clarix.domain.MedStatus.TAKEN).count();
        int exMin = exerciseLogs
            .findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since).stream()
            .mapToInt(com.clarix.domain.ExerciseLog::getDurationMin).sum();

        var moods = symptomLogs.findByPatientIdAndLogDateGreaterThanEqualOrderByLogDateAsc(patientId, sinceDate);
        var moodScores = moods.stream()
            .filter(m -> m.getMoodScore() != null)
            .mapToInt(SymptomLog::getMoodScore).toArray();
        Double avgMood = moodScores.length == 0 ? null
            : Math.round(java.util.Arrays.stream(moodScores).average().orElse(0) * 10.0) / 10.0;

        return new PatientAverages(
            days, sinceDate, hasRx,
            Math.round(meals * 10.0 / days) / 10.0,
            Math.round(meds  * 10.0 / days) / 10.0,
            Math.round(exMin * 10.0 / days) / 10.0,
            avgMood
        );
    }

    /** 7일치 운동 분/일 (오늘 끝, 0이면 0). 라벨 = MM-dd. */
    public record DaySpark(String label, int minutes) {}
    public List<DaySpark> exerciseMinutesPerDay(UUID patientId, int days) {
        OffsetDateTime since = LocalDate.now().minusDays(days - 1L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        var logs = exerciseLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since);
        Map<LocalDate, Integer> sum = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            sum.put(LocalDate.now().minusDays(i), 0);
        }
        for (var e : logs) {
            var d = e.getLoggedAt().toLocalDate();
            if (sum.containsKey(d)) sum.merge(d, e.getDurationMin(), Integer::sum);
        }
        List<DaySpark> out = new ArrayList<>();
        var fmt = java.time.format.DateTimeFormatter.ofPattern("M/d");
        for (var e : sum.entrySet()) {
            out.add(new DaySpark(e.getKey().format(fmt), e.getValue()));
        }
        return out;
    }

    /** 환자 상세 헤더 — 요약 / 특이사항 / 차팅 요약 한 번에. */
    public record PatientHeader(
        String name, String email, String hospitalName,
        OffsetDateTime registeredAt,
        int activePrescriptions,
        Integer adherence7d,
        Integer adherenceTakenCount,
        Integer adherenceExpectedCount,
        Double avgMood7d,
        Integer phq9Score, Severity phq9Severity, OffsetDateTime phq9At,
        Emotion lastMoodEmotion, String lastMoodJournal, java.time.LocalDate lastMoodDate,
        OffsetDateTime lastMedLogAt,
        OffsetDateTime lastSoapAt,
        int totalSoapCount,
        ClinicalNote latestSoap,
        java.util.List<Alert> alerts
    ) {}

    public record Alert(String tone, String text) {}

    public PatientHeader patientHeader(UUID patientId) {
        User patient = users.findById(patientId).orElseThrow();

        var rxList = prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId);
        int totalSlots = rxList.stream().mapToInt(p -> p.scheduleSlots().size()).sum();

        LocalDate sinceDate = LocalDate.now().minusDays(6);
        OffsetDateTime since = sinceDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        var medLogs7 = medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since);
        long taken = medLogs7.stream().filter(l -> l.getStatus() == com.clarix.domain.MedStatus.TAKEN).count();
        int expected = totalSlots * 7;
        Integer adherence = expected == 0 ? null : (int) Math.round(taken * 100.0 / expected);
        OffsetDateTime lastMedLog = medLogs7.stream()
            .map(MedicationLog::getTakenAt)
            .max(java.util.Comparator.naturalOrder()).orElse(null);

        var moods7 = symptomLogs.findByPatientIdAndLogDateGreaterThanEqualOrderByLogDateAsc(patientId, sinceDate);
        Double avgMood = null;
        if (!moods7.isEmpty()) {
            var ms = moods7.stream().filter(m -> m.getMoodScore() != null).mapToInt(SymptomLog::getMoodScore).toArray();
            if (ms.length > 0) {
                double avg = java.util.Arrays.stream(ms).average().orElse(0);
                avgMood = Math.round(avg * 10.0) / 10.0;
            }
        }
        var latestMood = moods7.stream()
            .max(java.util.Comparator.comparing(SymptomLog::getLogDate))
            .orElse(null);

        var phq = assessments.latest(patientId).orElse(null);

        var noteList = notes.findByPatientIdOrderByCreatedAtDesc(patientId);
        ClinicalNote latestSoap = noteList.isEmpty() ? null : noteList.get(0);

        java.util.List<Alert> alerts = new ArrayList<>();
        if (adherence != null) {
            if (adherence < 50)      alerts.add(new Alert("danger", "최근 7일 복약 " + adherence + "% — 긴급"));
            else if (adherence < 80) alerts.add(new Alert("warn",   "최근 7일 복약 " + adherence + "% — 저복약"));
        }
        if (phq != null) {
            if (phq.getSeverity() == Severity.SEVERE || phq.getSeverity() == Severity.MODERATELY_SEVERE) {
                alerts.add(new Alert("danger", "PHQ-9 " + phq.getSeverity().label() + " (" + phq.getTotalScore() + "점)"));
            } else if (phq.getSeverity() == Severity.MODERATE) {
                alerts.add(new Alert("warn", "PHQ-9 중등도 (" + phq.getTotalScore() + "점)"));
            }
        }
        if (latestMood != null && latestMood.getEmotion() != null) {
            var em = latestMood.getEmotion();
            if (em == Emotion.DOWN || em == Emotion.SAD || em == Emotion.ANGRY) {
                alerts.add(new Alert("warn",
                    "최근 감정: " + em.emoji() + " " + em.label()
                    + (latestMood.getJournal() != null && !latestMood.getJournal().isBlank()
                        ? " — " + (latestMood.getJournal().length() > 28
                            ? latestMood.getJournal().substring(0, 28) + "…"
                            : latestMood.getJournal())
                        : "")));
            }
        }
        if (latestMood == null) {
            alerts.add(new Alert("muted", "최근 7일 감정 기록 없음"));
        } else {
            long gap = java.time.temporal.ChronoUnit.DAYS.between(latestMood.getLogDate(), LocalDate.now());
            if (gap >= 3) alerts.add(new Alert("muted", gap + "일 연속 감정 미기록"));
        }
        if (lastMedLog == null && totalSlots > 0) {
            alerts.add(new Alert("warn", "최근 7일 복약 기록 없음"));
        }

        // 운동 부족 알림 (7일)
        OffsetDateTime since7 = LocalDate.now().minusDays(6L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        var ex7 = exerciseLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since7);
        if (ex7.isEmpty()) {
            alerts.add(new Alert("muted", "최근 7일 운동 기록 없음"));
        }

        // 용량 피드백 패턴 (7일) — TOO_LOW / TOO_HIGH 자가보고
        long lowCount  = medLogs7.stream()
            .filter(l -> l.getDosageFeedback() == com.clarix.domain.DosageFeedback.TOO_LOW).count();
        long highCount = medLogs7.stream()
            .filter(l -> l.getDosageFeedback() == com.clarix.domain.DosageFeedback.TOO_HIGH).count();
        if (lowCount >= 2) {
            alerts.add(new Alert("warn", "환자가 용량 부족 보고 " + lowCount + "회 — 증량 검토"));
        }
        if (highCount >= 2) {
            alerts.add(new Alert("warn", "환자가 용량 과다 보고 " + highCount + "회 — 감량 검토"));
        }

        return new PatientHeader(
            patient.getName(), patient.getEmail(),
            patient.getHospital() != null ? patient.getHospital().getName() : null,
            patient.getCreatedAt(),
            rxList.size(),
            adherence, (int) taken, expected,
            avgMood,
            phq == null ? null : phq.getTotalScore(),
            phq == null ? null : phq.getSeverity(),
            phq == null ? null : phq.getCreatedAt(),
            latestMood == null ? null : latestMood.getEmotion(),
            latestMood == null ? null : latestMood.getJournal(),
            latestMood == null ? null : latestMood.getLogDate(),
            lastMedLog,
            latestSoap == null ? null : latestSoap.getCreatedAt(),
            noteList.size(),
            latestSoap,
            alerts
        );
    }

    /** DOC-04 치료 이력 타임라인 — 약/감정/SOAP/PHQ-9 이벤트를 시간순 머지. */
    public record TimelineEvent(OffsetDateTime when, String kind, String text,
                                 Emotion emotion, String journal) {}

    public List<TimelineEvent> timelineEvents(UUID patientId, int limit) {
        java.time.LocalDate sinceDate = java.time.LocalDate.now().minusDays(13L);
        OffsetDateTime since = sinceDate.atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();

        List<TimelineEvent> out = new ArrayList<>();

        // 일자별 복약 합계 (taken count)
        Map<java.time.LocalDate, Integer> medByDay = new HashMap<>();
        for (var l : medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since)) {
            if (l.getStatus() != com.clarix.domain.MedStatus.TAKEN) continue;
            medByDay.merge(l.getTakenAt().toLocalDate(), 1, Integer::sum);
        }
        for (var e : medByDay.entrySet()) {
            OffsetDateTime when = e.getKey().atTime(20, 0)
                .atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            out.add(new TimelineEvent(when, "med", e.getValue() + "회 복약", null, null));
        }

        // 감정 + 일기 — 이모지/저널은 record 필드로 분리해서 템플릿이 시각화 가능
        for (var s : symptomLogs.findByPatientIdAndLogDateGreaterThanEqualOrderByLogDateAsc(patientId, sinceDate)) {
            OffsetDateTime when = s.getLogDate().atTime(12, 0)
                .atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            out.add(new TimelineEvent(when, "mood", null, s.getEmotion(), s.getJournal()));
        }

        // SOAP
        for (var n : notes.findByPatientIdOrderByCreatedAtDesc(patientId)) {
            String snippet = n.getAssessment() != null ? n.getAssessment()
                : (n.getSubjective() != null ? n.getSubjective() : "");
            if (snippet.length() > 40) snippet = snippet.substring(0, 40) + "…";
            out.add(new TimelineEvent(n.getCreatedAt(), "soap", "SOAP — " + snippet, null, null));
        }

        // PHQ-9
        for (var a : assessments.history(patientId)) {
            out.add(new TimelineEvent(a.getCreatedAt(), "phq9",
                "PHQ-9 " + a.getTotalScore() + " · " + a.getSeverity().label(), null, null));
        }

        // 식사
        for (var m : mealLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since)) {
            String text = m.getKind().emoji() + " " + m.getKind().label()
                + (m.getNote() != null && !m.getNote().isBlank() ? " — " + m.getNote() : "");
            out.add(new TimelineEvent(m.getLoggedAt(), "meal", text, null, null));
        }

        // 운동
        for (var ex : exerciseLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since)) {
            String text = "🏃 " + ex.getKind() + " · " + ex.getDurationMin() + "분"
                + (ex.getNote() != null && !ex.getNote().isBlank() ? " — " + ex.getNote() : "");
            out.add(new TimelineEvent(ex.getLoggedAt(), "exercise", text, null, null));
        }

        out.sort((a, b) -> b.when().compareTo(a.when()));
        return out.size() > limit ? out.subList(0, limit) : out;
    }
}
