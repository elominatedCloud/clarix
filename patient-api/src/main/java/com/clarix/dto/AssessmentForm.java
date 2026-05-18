package com.clarix.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

/**
 * PHQ-9 HTML form DTO. Controller는 이 DTO를 검증/변환하고,
 * Service 계층은 Map<String, Integer> 형태의 점수 데이터만 받는다.
 */
@Data
public class AssessmentForm {
    private Integer q1;
    private Integer q2;
    private Integer q3;
    private Integer q4;
    private Integer q5;
    private Integer q6;
    private Integer q7;
    private Integer q8;
    private Integer q9;
    private String onboarding;

    public Map<String, Integer> toAnswers() {
        Map<String, Integer> answers = new LinkedHashMap<>();
        Integer[] values = {q1, q2, q3, q4, q5, q6, q7, q8, q9};
        for (int i = 0; i < values.length; i++) {
            Integer value = values[i];
            if (value == null) {
                throw new IllegalArgumentException("missing q" + (i + 1));
            }
            answers.put("q" + (i + 1), value);
        }
        return answers;
    }

    public boolean isOnboardingFlow() {
        return "1".equals(onboarding);
    }
}
