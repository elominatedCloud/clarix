package com.clarix.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExerciseLogForm {
    @NotBlank(message = "운동 종류를 입력하세요")
    @Size(max = 60, message = "운동 종류는 60자 이하여야 합니다")
    private String kind;

    @Min(value = 1, message = "운동 시간은 1분 이상이어야 합니다")
    @Max(value = 720, message = "운동 시간은 720분 이하여야 합니다")
    private int durationMin;

    @Size(max = 500, message = "운동 메모는 500자 이하여야 합니다")
    private String note;
}
