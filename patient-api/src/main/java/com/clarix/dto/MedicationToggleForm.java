package com.clarix.dto;

import com.clarix.domain.MedStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicationToggleForm {
    @NotBlank(message = "약 정보가 비어 있습니다")
    @Size(max = 100, message = "약 이름은 100자 이하여야 합니다")
    private String medicationName;

    @NotBlank(message = "복약 시간이 비어 있습니다")
    @Pattern(regexp = "morning|noon|evening|asneeded", message = "복약 시간이 올바르지 않습니다")
    private String slot;

    private MedStatus status;

    @Size(max = 200, message = "돌아갈 경로가 너무 깁니다")
    private String returnTo = "/patient/";
}
