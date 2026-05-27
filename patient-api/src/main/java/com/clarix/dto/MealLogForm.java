package com.clarix.dto;

import com.clarix.domain.MealKind;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MealLogForm {
    @NotNull(message = "식사 종류를 선택하세요")
    private MealKind kind;

    @Size(max = 500, message = "식사 메모는 500자 이하여야 합니다")
    private String note;
}
