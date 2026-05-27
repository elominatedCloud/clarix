package com.clarix.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Emotion;
import com.clarix.domain.ExerciseLog;
import com.clarix.domain.Hospital;
import com.clarix.domain.MealKind;
import com.clarix.domain.MealLog;
import com.clarix.domain.MedStatus;
import com.clarix.domain.MedicationLog;
import com.clarix.domain.Prescription;
import com.clarix.domain.SymptomLog;
import com.clarix.domain.User;
import com.clarix.repo.ExerciseLogRepository;
import com.clarix.repo.HospitalRepository;
import com.clarix.repo.MealLogRepository;
import com.clarix.repo.MedicationLogRepository;
import com.clarix.repo.PrescriptionRepository;
import com.clarix.repo.SymptomLogRepository;
import com.clarix.repo.UserRepository;

@Service
public class PatientService {

    private final PrescriptionRepository prescriptions;
    private final MedicationLogRepository medLogs;
    private final SymptomLogRepository symptomLogs;
    private final UserRepository users;
    private final HospitalRepository hospitals;
    private final MealLogRepository mealLogs;
    private final ExerciseLogRepository exerciseLogs;

    public PatientService(PrescriptionRepository prescriptions, MedicationLogRepository medLogs,
                          SymptomLogRepository symptomLogs, UserRepository users,
                          HospitalRepository hospitals,
                          MealLogRepository mealLogs, ExerciseLogRepository exerciseLogs) {
        this.prescriptions = prescriptions;
        this.medLogs = medLogs;
        this.symptomLogs = symptomLogs;
        this.users = users;
        this.hospitals = hospitals;
        this.mealLogs = mealLogs;
        this.exerciseLogs = exerciseLogs;
    }

    /* ---- meal / exercise ---- */
    @Transactional
    public MealLog logMeal(User patient, MealKind kind, String note) {
        MealLog m = new MealLog();
        m.setPatient(patient);
        m.setKind(kind);
        m.setNote(note == null || note.isBlank() ? null : note.trim());
        return mealLogs.save(m);
    }

    @Transactional
    public ExerciseLog logExercise(User patient, String kind, int durationMin, String note) {
        ExerciseLog e = new ExerciseLog();
        e.setPatient(patient);
        e.setKind(kind == null ? "" : kind.trim());
        e.setDurationMin(Math.max(0, durationMin));
        e.setNote(note == null || note.isBlank() ? null : note.trim());
        return exerciseLogs.save(e);
    }

    public List<MealLog> recentMeals(UUID patientId, int days) {
        OffsetDateTime since = LocalDate.now().minusDays(days - 1L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        return mealLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since);
    }

    public List<ExerciseLog> recentExercises(UUID patientId, int days) {
        OffsetDateTime since = LocalDate.now().minusDays(days - 1L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        return exerciseLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, since);
    }

    /* ---- prescriptions ---- */
    public List<Prescription> activePrescriptions(UUID patientId) {
        return prescriptions.findByPatientIdAndActiveTrueOrderByCreatedAtAsc(patientId);
    }

    @Transactional
    public Prescription addPrescription(User patient, String name, List<String> schedule, int daysSupply) {
        return addPrescription(patient, name, schedule, daysSupply, null);
    }

    @Transactional
    public Prescription addPrescription(User patient, String name, List<String> schedule,
                                        int daysSupply, String storageLocation) {
        Prescription p = new Prescription();
        p.setPatient(patient);
        p.setMedicationName(name);
        p.setScheduleSlots(schedule);
        p.setDaysSupply(daysSupply);
        if (storageLocation != null && !storageLocation.isBlank()) {
            p.setStorageLocation(storageLocation.trim());
        }
        return prescriptions.save(p);
    }

    @Transactional
    public void updateStorage(UUID patientId, UUID prescriptionId, String storageLocation, String photoUrl) {
        Prescription p = requireOwnPrescription(patientId, prescriptionId);
        p.setStorageLocation(storageLocation == null || storageLocation.isBlank() ? null : storageLocation.trim());
        if (photoUrl != null) p.setStoragePhotoUrl(photoUrl.isBlank() ? null : photoUrl.trim());
        prescriptions.save(p);
    }

    @Transactional
    public void deactivatePrescription(UUID patientId, UUID prescriptionId) {
        Prescription p = requireOwnPrescription(patientId, prescriptionId);
        p.setActive(false);
        prescriptions.save(p);
    }

    private Prescription requireOwnPrescription(UUID patientId, UUID prescriptionId) {
        Prescription p = prescriptions.findById(prescriptionId).orElseThrow();
        if (!p.getPatient().getId().equals(patientId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        return p;
    }

    /* ---- medication logs ---- */
    public List<MedicationLog> todayLogs(UUID patientId) {
        OffsetDateTime start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        return medLogs.findByPatientIdAndTakenAtBetween(patientId, start, start.plusDays(1));
    }

    /** 테스트용: 오늘 로그(약 복용·기분·식사·운동) 모두 삭제. */
    @Transactional
    public void resetToday(UUID patientId) {
        OffsetDateTime start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime end = start.plusDays(1);
        medLogs.deleteAll(medLogs.findByPatientIdAndTakenAtBetween(patientId, start, end));
        symptomLogs.findByPatientIdAndLogDate(patientId, LocalDate.now()).ifPresent(symptomLogs::delete);
        mealLogs.deleteAll(mealLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, start));
        exerciseLogs.deleteAll(exerciseLogs.findByPatientIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(patientId, start));
    }

    @Transactional
    public MedicationLog logMedication(User patient, String medName, String slot, MedStatus status) {
        return logMedicationOn(patient, medName, slot, status, LocalDate.now());
    }

    @Transactional
    public MedicationLog logMedicationOn(User patient, String medName, String slot, MedStatus status, LocalDate date) {
        // PRN(필요시 복용)은 매번 새 로그, 현재 시각 그대로 기록 (시간대별 dedupe 안 함)
        if ("asneeded".equalsIgnoreCase(slot)) {
            MedicationLog x = new MedicationLog();
            x.setPatient(patient);
            x.setMedicationName(medName);
            x.setTakenAt(OffsetDateTime.now());
            x.setStatus(status);
            return medLogs.save(x);
        }
        int hour = switch (slot) { case "morning" -> 8; case "noon" -> 13; case "evening" -> 20; default -> 12; };
        OffsetDateTime at = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime windowStart = at.withMinute(0).withSecond(0);
        OffsetDateTime windowEnd = windowStart.plusHours(1).minusSeconds(1);

        // 같은 약/같은 시간대는 한 번만 기록하고 상태만 갱신합니다.
        // 사용자가 실수로 여러 번 눌러도 오늘 아침 약 로그가 중복 생성되지 않습니다.
        Optional<MedicationLog> existing = medLogs
            .findFirstByPatientIdAndMedicationNameAndTakenAtBetween(patient.getId(), medName, windowStart, windowEnd);
        MedicationLog m = existing.orElseGet(() -> {
            MedicationLog x = new MedicationLog();
            x.setPatient(patient);
            x.setMedicationName(medName);
            x.setTakenAt(at);
            return x;
        });
        m.setStatus(status);
        return medLogs.save(m);
    }

    public record MissedSlot(String medicationName, String slot) {}

    public List<MissedSlot> yesterdayMissedSlots(UUID patientId) {
        LocalDate y = LocalDate.now().minusDays(1);
        OffsetDateTime ystart = y.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime yend   = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        var logs = medLogs.findByPatientIdAndTakenAtBetween(patientId, ystart, yend);
        java.util.Set<String> takenKeys = new java.util.HashSet<>();
        for (var l : logs) {
            if (l.getStatus() != com.clarix.domain.MedStatus.TAKEN) continue;
            int h = l.getTakenAt().atZoneSameInstant(ZoneId.systemDefault()).getHour();
            String s = h < 11 ? "morning" : h < 17 ? "noon" : "evening";
            takenKeys.add(l.getMedicationName() + "|" + s);
        }
        List<MissedSlot> out = new java.util.ArrayList<>();
        for (Prescription p : activePrescriptions(patientId)) {
            for (String slot : p.scheduleSlots()) {
                // PRN(필요시 복용)은 일일 의무가 아님 — 어제 미복용 단계에 포함하지 않음
                if ("asneeded".equalsIgnoreCase(slot)) continue;
                if (!takenKeys.contains(p.getMedicationName() + "|" + slot)) {
                    out.add(new MissedSlot(p.getMedicationName(), slot));
                }
            }
        }
        return out;
    }

    /** 활성 처방 중 가장 일찍 끝나는 종료일. 처방 없으면 empty. */
    public Optional<LocalDate> nextRefillDate(UUID patientId) {
        return activePrescriptions(patientId).stream()
            .map(Prescription::endDate)
            .min(java.util.Comparator.naturalOrder());
    }

    /** 가장 빠른 처방 종료까지 남은 일수. 처방 없으면 -1. */
    public int daysUntilRefill(UUID patientId) {
        return nextRefillDate(patientId)
            .map(d -> (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), d))
            .orElse(-1);
    }

    /** 처방별 누적 (taken, expected) — 시작일부터 오늘까지. */
    public record RxAdherence(int taken, int expected) {}

    public RxAdherence cumulativeAdherence(UUID patientId, com.clarix.domain.Prescription rx) {
        java.time.LocalDate start = rx.getCreatedAt().toLocalDate();
        long daysElapsed = Math.min(
            java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now()) + 1,
            rx.getDaysSupply()
        );
        if (daysElapsed < 0) daysElapsed = 0;
        int slotsPerDay = rx.scheduleSlots().size();
        int expected = (int) (slotsPerDay * daysElapsed);
        OffsetDateTime since = start.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        int taken = (int) medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since).stream()
            .filter(l -> l.getStatus() == com.clarix.domain.MedStatus.TAKEN)
            .filter(l -> l.getMedicationName().equals(rx.getMedicationName()))
            .count();
        return new RxAdherence(taken, expected);
    }

    /** 7일 복약률 (taken / expected). 처방 없으면 null. */
    public Integer adherence7Days(UUID patientId) {
        var rxList = activePrescriptions(patientId);
        int totalSlots = rxList.stream().mapToInt(p -> p.scheduleSlots().size()).sum();
        if (totalSlots == 0) return null;
        OffsetDateTime since = LocalDate.now().minusDays(6L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        long taken = medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since)
            .stream().filter(l -> l.getStatus() == com.clarix.domain.MedStatus.TAKEN).count();
        int expected = totalSlots * 7;
        return (int) Math.round(taken * 100.0 / expected);
    }

    @Transactional
    public void recordDosageFeedback(UUID patientId, UUID logId, com.clarix.domain.DosageFeedback fb) {
        var log = medLogs.findById(logId).orElseThrow();
        if (!log.getPatient().getId().equals(patientId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "no permission");
        }
        log.setDosageFeedback(fb);
        medLogs.save(log);
    }

    /** 오늘 TAKEN인데 dosageFeedback이 아직 없는 로그 (피드백 카드용). */
    public List<MedicationLog> todayLogsAwaitingFeedback(UUID patientId) {
        return todayLogs(patientId).stream()
            .filter(l -> l.getStatus() == com.clarix.domain.MedStatus.TAKEN)
            .filter(l -> l.getDosageFeedback() == null)
            .toList();
    }

    public List<MedicationLog> recentLogs(UUID patientId, int days) {
        OffsetDateTime since = LocalDate.now().minusDays(days - 1L)
            .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        return medLogs.findByPatientIdAndTakenAtGreaterThanEqual(patientId, since);
    }

    /* ---- symptom logs ---- */
    public Optional<SymptomLog> todayMood(UUID patientId) {
        return symptomLogs.findByPatientIdAndLogDate(patientId, LocalDate.now());
    }

    @Transactional
    public SymptomLog upsertMood(User patient, Integer moodScore, String symptomsJson, String note) {
        LocalDate today = LocalDate.now();
        SymptomLog row = symptomLogs.findByPatientIdAndLogDate(patient.getId(), today)
            .orElseGet(() -> {
                SymptomLog x = new SymptomLog();
                x.setPatient(patient);
                x.setLogDate(today);
                return x;
            });
        row.setMoodScore(moodScore);
        row.setSymptoms(symptomsJson);
        row.setNote(note);
        return symptomLogs.save(row);
    }

    @Transactional
    public SymptomLog upsertEmotion(User patient, Emotion emotion, String journal) {
        LocalDate today = LocalDate.now();
        // 감정 기록은 하루 1행으로 유지합니다. 이미 있으면 update, 없으면 insert.
        SymptomLog row = symptomLogs.findByPatientIdAndLogDate(patient.getId(), today)
            .orElseGet(() -> {
                SymptomLog x = new SymptomLog();
                x.setPatient(patient);
                x.setLogDate(today);
                return x;
            });
        row.setEmotion(emotion);
        row.setJournal(journal);
        // 의사용 차트는 숫자 moodScore를 사용하므로 감정 enum 점수와 동기화합니다.
        row.setMoodScore(emotion.score());
        return symptomLogs.save(row);
    }

    public List<SymptomLog> recentMoods(UUID patientId, int days) {
        return symptomLogs.findByPatientIdAndLogDateGreaterThanEqualOrderByLogDateAsc(
            patientId, LocalDate.now().minusDays(days - 1L));
    }

    public List<SymptomLog> moodsBetween(UUID patientId, LocalDate from, LocalDate to) {
        return symptomLogs.findByPatientIdAndLogDateBetweenOrderByLogDateAsc(patientId, from, to);
    }

    @Transactional
    public boolean deleteMoodOn(UUID patientId, LocalDate date) {
        return symptomLogs.findByPatientIdAndLogDate(patientId, date)
            .map(m -> { symptomLogs.delete(m); return true; })
            .orElse(false);
    }

    /* ---- hospital ---- */
    public List<Hospital> listHospitals(boolean partnered) {
        return hospitals.findByPartneredOrderByDistanceKmAsc(partnered);
    }

    public List<User> doctorsAtHospital(UUID hospitalId) {
        return users.findByHospitalIdAndRole(hospitalId, com.clarix.domain.Role.DOCTOR);
    }

    @Transactional
    public void setHospital(User patient, UUID hospitalId) {
        Hospital h = hospitals.findById(hospitalId).orElseThrow();
        patient.setHospital(h);
        users.save(patient);
    }
}
