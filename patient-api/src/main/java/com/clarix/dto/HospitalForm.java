package com.clarix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HospitalForm {
    @NotBlank(message = "병원명을 입력하세요")
    @Size(max = 100, message = "병원명은 100자 이하여야 합니다")
    private String name;

    @Size(max = 200, message = "주소는 200자 이하여야 합니다")
    private String address;

    @Size(max = 100, message = "진료과는 100자 이하여야 합니다")
    private String specialty;
    private boolean partnered = true;
}
