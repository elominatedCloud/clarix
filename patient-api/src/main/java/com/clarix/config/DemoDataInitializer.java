package com.clarix.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Assessment;
import com.clarix.domain.ClinicalNote;
import com.clarix.domain.Emotion;
import com.clarix.domain.ExerciseLog;
import com.clarix.domain.Hospital;
import com.clarix.domain.MealKind;
import com.clarix.domain.MealLog;
import com.clarix.domain.MedStatus;
import com.clarix.domain.MedicationLog;
import com.clarix.domain.Permission;
import com.clarix.domain.Prescription;
import com.clarix.domain.Role;
import com.clarix.domain.Severity;
import com.clarix.domain.SymptomLog;
import com.clarix.domain.User;
import com.clarix.repo.AssessmentRepository;
import com.clarix.repo.ClinicalNoteRepository;
import com.clarix.repo.ExerciseLogRepository;
import com.clarix.repo.HospitalRepository;
import com.clarix.repo.MealLogRepository;
import com.clarix.repo.MedicationLogRepository;
import com.clarix.repo.PermissionRepository;
import com.clarix.repo.PrescriptionRepository;
import com.clarix.repo.SymptomLogRepository;
import com.clarix.repo.UserRepository;
import com.clarix.service.JsonStore;
import com.clarix.service.PasswordHasher;

/**
 * 첫 부팅 시 MySQL 데모 데이터 자동 시드.
 * users 테이블에 데이터가 있으면 기존 사용자 데이터를 보존하기 위해 skip.
 *
 * 가입 계정:
 *   doctor:   dr.kim@clarix.demo  / clarix1234
 *   patient1: patient1@clarix.demo / clarix1234   (순응도 ~90%, 기분 안정)
 *   patient2: patient2@clarix.demo / clarix1234   (순응도 ~60%, 기분 변동)
 *   patient3: patient3@clarix.demo / clarix1234   (순응도 ~30%, 우울 위험)
 *   patient4: patient4@clarix.demo / clarix1234   (오늘 약 미복용 — 봉투 strip 테스트)
 */
@Configuration
public class DemoDataInitializer {

    @Bean
    public CommandLineRunner seedDemoData(
        UserRepository users,
        HospitalRepository hospitals,
        PermissionRepository permissions,
        PrescriptionRepository prescriptions,
        MedicationLogRepository medLogs,
        SymptomLogRepository symptomLogs,
        AssessmentRepository assessments,
        ClinicalNoteRepository notes,
        MealLogRepository mealLogs,
        ExerciseLogRepository exerciseLogs,
        PasswordHasher hasher,
        JsonStore json
    ) {
        return new SeedRunner(users, hospitals, permissions, prescriptions,
                              medLogs, symptomLogs, assessments, notes,
                              mealLogs, exerciseLogs, hasher, json)::run;
    }

    private record SeedRunner(
        UserRepository users,
        HospitalRepository hospitals,
        PermissionRepository permissions,
        PrescriptionRepository prescriptions,
        MedicationLogRepository medLogs,
        SymptomLogRepository symptomLogs,
        AssessmentRepository assessments,
        ClinicalNoteRepository notes,
        MealLogRepository mealLogs,
        ExerciseLogRepository exerciseLogs,
        PasswordHasher hasher,
        JsonStore json
    ) {
        @Transactional
        void run(String... args) {
            if (users.count() > 0) return; // 이미 데이터 있으면 skip
            List<Hospital> hl = hospitals.findAll();
            if (hl.isEmpty()) {
                hl = seedHospitals();
            }

            String pw = hasher.hash("clarix1234");
            Hospital hospital = hl.get(0);

            saveUser("admin@clarix.demo", "Clarix 관리자", Role.ADMIN, pw, null);
            User doctor = saveUser("dr.kim@clarix.demo", "김의사", Role.DOCTOR, pw, hospital);
            saveUser("nurse@clarix.demo", "이간호", Role.NURSE, pw, hospital);
            saveUser("tech@clarix.demo", "박기사", Role.TECHNICIAN, pw, hospital);
            saveUser("desk@clarix.demo", "최데스크", Role.RECEPTIONIST, pw, hospital);
            User p1 = saveUser("patient1@clarix.demo", "김환자", Role.PATIENT, pw, hospital);
            User p2 = saveUser("patient2@clarix.demo", "이환자", Role.PATIENT, pw, hospital);
            User p3 = saveUser("patient3@clarix.demo", "박환자", Role.PATIENT, pw, hospital);
            User p4 = saveUser("patient4@clarix.demo", "정환자", Role.PATIENT, pw, hospital);

            grantPerm(p1, doctor);
            grantPerm(p2, doctor);
            grantPerm(p3, doctor);
            grantPerm(p4, doctor);

            // daysSupply가 7 이하면 today 메인이 'hospital_day' 모드로 진입 → 데모 다양성
            seedFor(p1, "콘서타 18mg",    13, Emotion.CALM, 14, 30);   // 여유
            seedFor(p2, "아빌리파이 5mg",  8, Emotion.SAD,   7, 14);   // 보통
            seedFor(p3, "리스페달 1mg",    4, Emotion.DOWN,  3,  5);   // 곧 refill (hospital_day)

            // 약 상호작용 시연용 추가 처방
            // p2: 아빌리파이 + 파록세틴 → CYP2D6 상호작용 (warn)
            addExtraRx(p2, "파록세틴 20mg", List.of("morning"), 30);
            // p3: 리스페달 + 아빌리파이 → 항정신병약 중복 (warn)
            addExtraRx(p3, "아빌리파이 5mg", List.of("evening"), 5);

            // p4: 봉투 strip 테스트용 — 아침/점심/저녁 슬롯 골고루, 오늘 약은 한 알도 안 먹음.
            seedDueMedTestPatient(p4);

            // 14일치 식사·운동 시드 (환자별 패턴)
            seedMealsAndExercise(p1, "stable");
            seedMealsAndExercise(p2, "variable");
            seedMealsAndExercise(p3, "low");

            // PHQ-9
            assessments.save(makeAssessment(p1, hospital,  6, Severity.MILD));
            assessments.save(makeAssessment(p2, hospital, 12, Severity.MODERATE));
            assessments.save(makeAssessment(p3, hospital, 21, Severity.SEVERE));
            assessments.save(makeAssessment(p4, hospital,  5, Severity.MILD));

            // SOAP 1건씩
            notes.save(makeSoap(doctor, p1, "컨디션 안정", "복약 90%, 기분 4.0", "치료 반응 양호", "현 처방 유지"));
            notes.save(makeSoap(doctor, p2, "효과 변동성 호소", "복약 60%, 기분 2~4", "용량 조정 검토", "아빌리파이 7.5mg로 증량"));
            notes.save(makeSoap(doctor, p3, "복약 거부 빈번, 우울감 심함", "복약 30%, 기분 1~2, PHQ-9 21", "심각한 우울 — 약물 변경 필요", "리스페달 중단 검토, sertraline 50mg 시작"));
        }

        private List<Hospital> seedHospitals() {
            return List.of(
                saveHospital("마음정신건강의학과의원", "서울 강남구 테헤란로 152", "정신건강의학과", true, "1.2"),
                saveHospital("서울대학교병원 정신건강의학과", "서울 종로구 대학로 101", "정신건강의학과", true, "3.4"),
                saveHospital("연세아이정신건강의학과", "서울 서대문구 신촌로 134", "정신건강의학과", true, "4.1"),
                saveHospital("성북마음클리닉", "서울 성북구 동소문로 89", "정신건강의학과", true, "5.8"),
                saveHospital("판교웰니스의원", "경기 성남시 분당구 판교역로 235", "내과·정신건강의학과", true, "12.5"),
                saveHospital("하늘마음의원", "서울 마포구 양화로 188", "정신건강의학과", false, "2.7"),
                saveHospital("새벽병원 정신건강센터", "서울 영등포구 여의대로 24", "정신건강의학과", false, "6.9"),
                saveHospital("편안한 정신건강의원", "서울 송파구 올림픽로 240", "정신건강의학과", false, "8.3")
            );
        }

        private Hospital saveHospital(String name, String address, String specialty,
                                      boolean partnered, String distanceKm) {
            Hospital h = new Hospital();
            h.setName(name);
            h.setAddress(address);
            h.setSpecialty(specialty);
            h.setPartnered(partnered);
            h.setDistanceKm(new BigDecimal(distanceKm));
            return hospitals.save(h);
        }

        /**
         * 봉투 strip 테스트 환자.
         *   - 처방 3개: 아침/점심/저녁 슬롯에 분산 → 가로 strip에 morning/noon/evening PNG가 모두 보임
         *   - 어제까지: 모두 TAKEN (어제 미복용 단계 안 뜸)
         *   - 오늘: medication_log 0건 → dueSlot 윈도우라면 due_med 단계로 진입
         *   - 오늘 mood/식사/운동 미리 기록 → 봉투 단계 통과 후 done으로
         */
        private void seedDueMedTestPatient(User patient) {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now();

            // 슬롯별로 여러 약을 배치 — strip 큐가 채워지도록.
            //   morning: 에스시탈로프람 + 발프로산 + 비타민 D     (3알)
            //   noon   : 쿠에티아핀 + 발프로산                    (2알)
            //   evening: 쿠에티아핀 + 트라조돈 + 발프로산          (3알)
            addExtraRx(patient, "에스시탈로프람 10mg", List.of("morning"), 30);
            addExtraRx(patient, "발프로산 250mg",     List.of("morning", "noon", "evening"), 30);
            addExtraRx(patient, "비타민 D 1000IU",   List.of("morning"), 30);
            addExtraRx(patient, "쿠에티아핀 25mg",   List.of("noon", "evening"), 30);
            addExtraRx(patient, "트라조돈 50mg",     List.of("evening"), 30);
            // 필요시 복용 (PRN) — 위쪽 strip에 노출, 사용자가 직접 뜯어서 복용
            addExtraRx(patient, "로라제팜 0.5mg",    List.of("asneeded"), 30);
            addExtraRx(patient, "프로프라놀롤 10mg", List.of("asneeded"), 30);

            // 어제까지 7일 — 매 슬롯의 모든 약 TAKEN (어제 미복용 단계 안 걸리도록)
            record SlotPlan(String[] meds, int hour) {}
            SlotPlan[] plans = new SlotPlan[] {
                new SlotPlan(new String[]{"에스시탈로프람 10mg", "발프로산 250mg", "비타민 D 1000IU"}, 8),
                new SlotPlan(new String[]{"쿠에티아핀 25mg", "발프로산 250mg"}, 13),
                new SlotPlan(new String[]{"쿠에티아핀 25mg", "트라조돈 50mg", "발프로산 250mg"}, 20)
            };
            for (int dayBack = 7; dayBack >= 1; dayBack--) {
                LocalDate d = today.minusDays(dayBack);
                for (SlotPlan plan : plans) {
                    for (String name : plan.meds()) {
                        // 어제(dayBack=1) 저녁의 트라조돈은 일부러 누락 → yesterday_med 단계 시연
                        if (dayBack == 1 && plan.hour() == 20 && name.equals("트라조돈 50mg")) continue;
                        OffsetDateTime ts = d.atTime(plan.hour(), 0).atZone(zone).toOffsetDateTime();
                        MedicationLog m = new MedicationLog();
                        m.setPatient(patient);
                        m.setMedicationName(name);
                        m.setStatus(MedStatus.TAKEN);
                        m.setTakenAt(ts);
                        medLogs.save(m);
                    }
                }
            }
            // 오늘 PRN 로라제팜 1회 (~2시간 전) — PRN 잠금/카운트 시연
            OffsetDateTime prnAt = OffsetDateTime.now().minusHours(2);
            MedicationLog prn = new MedicationLog();
            prn.setPatient(patient);
            prn.setMedicationName("로라제팜 0.5mg");
            prn.setStatus(MedStatus.TAKEN);
            prn.setTakenAt(prnAt);
            medLogs.save(prn);
            // 오늘 정규 logs 의도적으로 비움 — due_med 단계 트리거

            // 오늘은 의도적으로 비움 — 위저드 모든 단계 (due_med → dosage_feedback → mood → meal → exercise)
            // 를 처음부터 시연·검증할 수 있도록.

            // 과거 7일치 식사/운동만 (차트용)
            for (int dayBack = 7; dayBack >= 1; dayBack--) {
                LocalDate d = today.minusDays(dayBack);
                addMeal(patient, MealKind.BREAKFAST, d.atTime(8, 0).atZone(zone).toOffsetDateTime());
                addMeal(patient, MealKind.LUNCH,     d.atTime(12, 30).atZone(zone).toOffsetDateTime());
                if (dayBack % 2 == 0) {
                    addExercise(patient, "걷기", 25, d.atTime(7, 30).atZone(zone).toOffsetDateTime());
                }
            }
        }

        private void seedMealsAndExercise(User patient, String pattern) {
            ZoneId zone = ZoneId.systemDefault();
            // 14일 — pattern별로 식사·운동 빈도 다름
            // stable: 매일 아침/점심/저녁 + 주 4회 운동
            // variable: 식사 가끔 누락 + 주 2회 운동
            // low: 식사 자주 누락 + 운동 거의 없음
            for (int dayBack = 13; dayBack >= 0; dayBack--) {
                LocalDate d = LocalDate.now().minusDays(dayBack);

                if (pattern.equals("stable") || (pattern.equals("variable") && dayBack % 3 != 0)) {
                    addMeal(patient, MealKind.BREAKFAST, d.atTime(8, 10).atZone(zone).toOffsetDateTime());
                    addMeal(patient, MealKind.LUNCH,     d.atTime(12, 30).atZone(zone).toOffsetDateTime());
                    addMeal(patient, MealKind.DINNER,    d.atTime(19, 0).atZone(zone).toOffsetDateTime());
                    if (dayBack % 4 == 0) {
                        addMeal(patient, MealKind.FRUIT, d.atTime(15, 0).atZone(zone).toOffsetDateTime());
                    }
                    if (pattern.equals("stable") && dayBack % 5 == 0) {
                        addMeal(patient, MealKind.DESSERT, d.atTime(20, 30).atZone(zone).toOffsetDateTime());
                    }
                } else if (pattern.equals("variable")) {
                    // 식사 일부 누락
                    addMeal(patient, MealKind.LUNCH, d.atTime(13, 0).atZone(zone).toOffsetDateTime());
                    if (dayBack % 2 == 0) {
                        addMeal(patient, MealKind.DINNER, d.atTime(19, 30).atZone(zone).toOffsetDateTime());
                    }
                } else {
                    // low: 거의 없음
                    if (dayBack % 3 == 0) {
                        addMeal(patient, MealKind.LUNCH, d.atTime(14, 0).atZone(zone).toOffsetDateTime());
                    }
                }

                // 운동
                if (pattern.equals("stable") && (dayBack % 7 == 0 || dayBack % 7 == 2 || dayBack % 7 == 4 || dayBack % 7 == 5)) {
                    addExercise(patient, "걷기",  30 + (dayBack % 3) * 10, d.atTime(7, 30).atZone(zone).toOffsetDateTime());
                }
                if (pattern.equals("stable") && dayBack % 7 == 3) {
                    addExercise(patient, "헬스", 60, d.atTime(19, 0).atZone(zone).toOffsetDateTime());
                }
                if (pattern.equals("variable") && dayBack % 5 == 1) {
                    addExercise(patient, "걷기", 20, d.atTime(8, 0).atZone(zone).toOffsetDateTime());
                }
                // low → 운동 0회
            }
        }

        private void addMeal(User patient, MealKind kind, OffsetDateTime at) {
            MealLog m = new MealLog();
            m.setPatient(patient);
            m.setKind(kind);
            m.setLoggedAt(at);
            mealLogs.save(m);
        }

        private void addExercise(User patient, String kind, int durationMin, OffsetDateTime at) {
            ExerciseLog e = new ExerciseLog();
            e.setPatient(patient);
            e.setKind(kind);
            e.setDurationMin(durationMin);
            e.setLoggedAt(at);
            exerciseLogs.save(e);
        }

        private void addExtraRx(User patient, String name, List<String> slots, int daysSupply) {
            Prescription rx = new Prescription();
            rx.setPatient(patient);
            rx.setMedicationName(name);
            rx.setScheduleSlots(slots);
            rx.setActive(true);
            rx.setDaysSupply(daysSupply);
            prescriptions.save(rx);
        }

        private User saveUser(String email, String name, Role role, String pwHash, Hospital hospital) {
            User u = new User();
            u.setEmail(email); u.setName(name); u.setRole(role); u.setPasswordHash(pwHash);
            u.setHospital(hospital);
            return users.save(u);
        }

        private void grantPerm(User patient, User doctor) {
            Permission p = new Permission();
            p.setPatient(patient); p.setDoctor(doctor); p.setActive(true);
            permissions.save(p);
        }

        /**
         * 처방 + 7일치 medication_logs (취한 횟수=takenCount) + symptom_logs (감정+점수 패턴)
         */
        private void seedFor(User patient, String medName, int takenCount, Emotion baseEmotion,
                             int phq9Bias, int daysSupply) {
            Prescription rx = new Prescription();
            rx.setPatient(patient);
            rx.setMedicationName(medName);
            rx.setScheduleSlots(List.of("morning", "evening"));
            rx.setActive(true);
            rx.setDaysSupply(daysSupply);
            prescriptions.save(rx);

            // 14 슬롯 (7일 × 2회). taken/missed 분포는 결정적.
            int total = 14;
            int missed = total - takenCount;
            ZoneId zone = ZoneId.systemDefault();
            for (int i = 0; i < total; i++) {
                int dayBack = 6 - (i / 2);
                int hour = (i % 2 == 0) ? 8 : 20;
                OffsetDateTime ts = LocalDate.now().minusDays(dayBack).atTime(hour, 0).atZone(zone).toOffsetDateTime();
                MedStatus status = (i < missed) ? MedStatus.MISSED : MedStatus.TAKEN;
                MedicationLog m = new MedicationLog();
                m.setPatient(patient); m.setMedicationName(medName);
                m.setStatus(status); m.setTakenAt(ts);
                medLogs.save(m);
            }

            // 7일 mood + emotion 일기
            String[] entries = baseEmotion == Emotion.CALM
                ? new String[]{"컨디션 좋음", "오전 산책 후 기분 좋음", "잘 잤음", "약 정시 복용", "친구 만남",
                               "운동 30분", "평온한 하루"}
                : baseEmotion == Emotion.SAD
                ? new String[]{"두통이 잦음", "약 효과 들쭉날쭉", "잠을 설침", "기분 변동 큼", "어지러움 있음",
                               "조금 나아짐", "여전히 피곤"}
                : new String[]{"무기력함", "잠을 자지 못함", "식욕 없음", "약 거부함", "외출 어려움",
                               "감정 가라앉음", "도움 필요 느낌"};
            for (int i = 0; i < 7; i++) {
                Emotion e = i % 3 == 0 ? baseEmotion
                    : (baseEmotion == Emotion.CALM ? Emotion.JOY
                       : baseEmotion == Emotion.SAD ? Emotion.TIRED : Emotion.ANGRY);
                SymptomLog s = new SymptomLog();
                s.setPatient(patient);
                s.setLogDate(LocalDate.now().minusDays(6 - i));
                s.setEmotion(e);
                s.setMoodScore(e.score());
                s.setJournal(entries[i]);
                Map<String, Boolean> sx = new HashMap<>();
                if (baseEmotion == Emotion.SAD || baseEmotion == Emotion.DOWN) sx.put("두통", true);
                if (baseEmotion == Emotion.DOWN) { sx.put("불면", true); sx.put("식욕저하", true); }
                s.setSymptoms(json.stringify(sx));
                symptomLogs.save(s);
            }
        }

        private Assessment makeAssessment(User patient, Hospital h, int score, Severity sev) {
            Assessment a = new Assessment();
            a.setPatient(patient); a.setHospital(h);
            a.setKind("PHQ-9"); a.setTotalScore(score); a.setSeverity(sev);
            // answers 분포 (총합 = score)
            Map<String, Integer> ans = new HashMap<>();
            int remaining = score;
            for (int i = 1; i <= 9; i++) {
                int v = Math.min(3, remaining / (10 - i));
                ans.put("q" + i, v);
                remaining -= v;
            }
            a.setAnswers(json.stringify(ans));
            return a;
        }

        private ClinicalNote makeSoap(User doctor, User patient, String s, String o, String a, String p) {
            ClinicalNote n = new ClinicalNote();
            n.setDoctor(doctor); n.setPatient(patient);
            n.setSubjective(s); n.setObjective(o); n.setAssessment(a); n.setPlan(p);
            return n;
        }
    }
}
