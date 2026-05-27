package com.clarix.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionForm {
    @NotBlank(message = "약 이름을 입력하세요")
    @Size(max = 100, message = "약 이름은 100자 이하여야 합니다")
    private String medicationName;

    @NotEmpty(message = "복약 시간을 하나 이상 선택하세요")
    private List<String> slots = new ArrayList<>();

    @Min(value = 1, message = "처방 일수는 1일 이상이어야 합니다")
    @Max(value = 180, message = "처방 일수는 180일 이하여야 합니다")
    private int daysSupply = 30;
}
