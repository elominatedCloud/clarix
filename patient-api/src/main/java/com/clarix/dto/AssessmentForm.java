package com.clarix.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * PHQ-9 HTML form DTO. Controller는 이 DTO를 검증/변환하고,
 * Service 계층은 Map<String, Integer> 형태의 점수 데이터만 받는다.
 */
@Data
public class AssessmentForm {
    @NotNull(message = "1번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q1;
    @NotNull(message = "2번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q2;
    @NotNull(message = "3번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q3;
    @NotNull(message = "4번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q4;
    @NotNull(message = "5번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q5;
    @NotNull(message = "6번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q6;
    @NotNull(message = "7번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q7;
    @NotNull(message = "8번 문항을 선택하세요")
    @Min(0) @Max(3)
    private Integer q8;
    @NotNull(message = "9번 문항을 선택하세요")
    @Min(0) @Max(3)
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
