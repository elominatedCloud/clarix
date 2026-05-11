package com.clarix.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * 약 상호작용(drug-drug interaction).
 *
 * 룰 셋은 정신건강의학과 + 일반 만성질환 진료에서 자주 만나는 임상 의약품 상호작용을 키워드 매칭으로 대표한 데모.
 * 룰 출처: FDA 라벨 / Drugs.com Interaction Checker / Lexicomp / Stahl's Essential Psychopharmacology / 식약처 공개 라벨.
 * 한국 상품명 + 성분명을 같이 매칭해서, 환자가 등록한 자유텍스트 약 이름에 키워드가 포함되면 매칭한다.
 *
 * 운영 시에는 외부 의약품 API(KIMS, DrugBank 등)를 권장.
 */
@Service
public class DrugInteractionService {

    public record Risk(String drugA, String drugB, String severity, String message) {}

    private record Rule(String a, String b, String severity, String message) {}

    private static final List<Rule> RULES = List.of(
        // ── 세로토닌 증후군 / MAOI 금기 ───────────────────────────────
        new Rule("MAO", "SSRI", "danger",
            "MAOI + SSRI — 세로토닌 증후군 위험. 절대 병용 금기 (세척 14일 필요)"),
        new Rule("MAO", "SNRI", "danger",
            "MAOI + SNRI — 세로토닌 증후군 위험. 절대 병용 금기"),
        new Rule("MAO", "트라마돌", "danger",
            "MAOI + 트라마돌 — 세로토닌 증후군 + 발작 위험"),
        new Rule("MAO", "린ezolid", "danger",
            "MAOI 병용 — 고혈압성 위기"),
        new Rule("MAO", "콘서타", "danger",
            "MAOI + 메틸페니데이트 — 고혈압성 위기"),
        new Rule("MAO", "암페타민", "danger",
            "MAOI + 암페타민 — 고혈압성 위기"),
        new Rule("MAO", "웰부트린", "danger",
            "MAOI + 부프로피온 — 고혈압성 위기, 절대 금기"),
        new Rule("SSRI", "트라마돌", "warn",
            "SSRI + 트라마돌 — 세로토닌 증후군 위험 증가"),
        new Rule("SSRI", "트립탄", "warn",
            "SSRI + 트립탄(편두통) — 세로토닌 증후군 위험"),
        new Rule("SSRI", "린ezolid", "danger",
            "린졸리드는 약한 MAOI — 세로토닌 증후군 위험"),

        // ── 자극제(ADHD) ────────────────────────────────────────────
        new Rule("콘서타", "MAO", "danger",
            "메틸페니데이트 + MAOI — 고혈압성 위기"),
        new Rule("콘서타", "와파린", "warn",
            "메틸페니데이트가 와파린 대사 영향, INR 모니터링"),

        // ── 리튬 ─────────────────────────────────────────────────
        new Rule("리튬", "이부프로펜", "danger",
            "리튬 + NSAID — 리튬 농도 상승, 독성 위험"),
        new Rule("리튬", "나프록센", "warn",
            "리튬 + NSAID — 리튬 농도 상승 가능"),
        new Rule("리튬", "셀레콕시브", "warn",
            "리튬 + COX-2 — 리튬 농도 상승 가능"),
        new Rule("리튬", "ACE", "warn",
            "리튬 + ACE 억제제 — 리튬 농도 상승"),
        new Rule("리튬", "ARB", "warn",
            "리튬 + ARB — 리튬 농도 상승"),
        new Rule("리튬", "Thiazide", "warn",
            "리튬 + Thiazide 이뇨제 — 리튬 농도 상승, 정기 농도 측정"),
        new Rule("리튬", "SSRI", "warn",
            "리튬 + SSRI — 세로토닌 증후군 위험 가능, 모니터링"),

        // ── 항정신병약 (typical/atypical) ────────────────────────────
        new Rule("리스페달", "아빌리파이", "warn",
            "항정신병약 중복 — 추체외로 부작용 가산"),
        new Rule("리스페달", "자이프렉사", "warn",
            "항정신병약 중복 — 진정·체중 증가 가산"),
        new Rule("리스페달", "세로켈", "warn",
            "항정신병약 중복 — 진정 가산"),
        new Rule("아빌리파이", "자이프렉사", "warn",
            "항정신병약 중복"),
        new Rule("할로페리돌", "리스페달", "warn",
            "QT 연장 위험 가산"),
        new Rule("할로페리돌", "아빌리파이", "warn",
            "QT 연장 위험"),
        new Rule("할로페리돌", "자이프렉사", "warn",
            "QT 연장 위험"),
        new Rule("클로자핀", "카르바마제핀", "danger",
            "골수억제 가산 — 무과립구증 위험. 병용 회피"),
        new Rule("클로자핀", "벤조", "warn",
            "클로자핀 + 벤조디아제핀 — 호흡억제·심정지 사례 보고"),
        new Rule("아빌리파이", "파록세틴", "warn",
            "CYP2D6 억제 — 아빌리파이 농도 상승, 용량 감량 고려"),
        new Rule("아빌리파이", "플루옥세틴", "warn",
            "CYP2D6 억제 — 아빌리파이 농도 상승, 용량 감량 고려"),

        // ── 벤조 / 오피오이드 ───────────────────────────────────────
        new Rule("벤조", "오피오이드", "danger",
            "FDA 블랙박스 — 호흡억제·진정·사망 위험"),
        new Rule("자낙스", "오피오이드", "danger",
            "알프라졸람 + 오피오이드 — 호흡억제 위험"),
        new Rule("벤조", "알코올", "danger",
            "벤조 + 알코올 — 의식 저하·호흡억제"),
        new Rule("자이프렉사", "벤조", "warn",
            "올란자핀 + 벤조 — 진정 가산, 호흡 모니터링"),

        // ── 와파린 ─────────────────────────────────────────────────
        new Rule("와파린", "이부프로펜", "danger",
            "출혈 위험 — NSAID 병용 회피"),
        new Rule("와파린", "나프록센", "danger",
            "출혈 위험 — NSAID"),
        new Rule("와파린", "아스피린", "warn",
            "이중항혈전 — 적응증 외에는 병용 회피"),
        new Rule("와파린", "SSRI", "warn",
            "SSRI가 혈소판 기능 저하 — 출혈 위험 증가"),
        new Rule("와파린", "아미오다론", "warn",
            "INR 변동 — 와파린 용량 약 30~50% 감량 필요"),
        new Rule("와파린", "심발타", "warn",
            "둘록세틴 — 출혈 위험 증가 (혈소판 기능)"),

        // ── 기분안정제 (라모트리진/발프로/카르바마제핀) ─────────────
        new Rule("발프로", "라모트리진", "warn",
            "라모트리진 농도 상승 — 발진(SJS) 위험, 용량 50% 감량 권장"),
        new Rule("카르바마제핀", "라모트리진", "warn",
            "라모트리진 농도 감소 — 효과 저하, 용량 조정"),
        new Rule("카르바마제핀", "와파린", "warn",
            "와파린 대사 유도 — INR 저하"),
        new Rule("카르바마제핀", "경구피임약", "warn",
            "피임약 효과 감소"),

        // ── 부프로피온 ─────────────────────────────────────────────
        new Rule("웰부트린", "MAO", "danger",
            "고혈압성 위기 위험"),

        // ── 심혈관 ────────────────────────────────────────────────
        new Rule("디곡신", "아미오다론", "warn",
            "디곡신 농도 상승 — 용량 50% 감량 권장"),
        new Rule("디곡신", "베라파밀", "warn",
            "디곡신 농도 상승"),
        new Rule("스타틴", "에리스로마이신", "warn",
            "스타틴 농도 상승 — 횡문근융해 위험"),
        new Rule("스타틴", "자몽", "warn",
            "자몽주스 — 일부 스타틴(심바스타틴/로바스타틴) 농도 상승"),

        // ── 당뇨 ─────────────────────────────────────────────────
        new Rule("메트포르민", "조영제", "warn",
            "조영제 — 신부전 시 젖산산증 위험, 검사 전 일시 중단")
    );

    public List<Risk> check(List<String> medicationNames) {
        List<Risk> hits = new ArrayList<>();
        if (medicationNames == null || medicationNames.size() < 2) return hits;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Rule r : RULES) {
            String matchA = findContains(medicationNames, r.a());
            String matchB = findContains(medicationNames, r.b());
            if (matchA != null && matchB != null && !matchA.equals(matchB)) {
                String key = matchA.compareTo(matchB) < 0 ? matchA + "|" + matchB : matchB + "|" + matchA;
                if (seen.add(key)) {
                    hits.add(new Risk(matchA, matchB, r.severity(), r.message()));
                }
            }
        }
        return hits;
    }

    private String findContains(List<String> names, String key) {
        for (String n : names) {
            if (n != null && n.toLowerCase().contains(key.toLowerCase())) return n;
        }
        return null;
    }
}
