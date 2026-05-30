package com.clarix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CareRequestForm {
    @NotBlank(message = "진료 요청 내용을 입력하세요")
    @Size(max = 500, message = "진료 요청 내용은 500자 이하여야 합니다")
    private String reason;

    @Size(max = 200, message = "돌아갈 경로가 너무 깁니다")
    private String returnTo = "/patient/";
}
