package com.clarix.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HospitalPickForm {
    @NotNull(message = "병원을 선택하세요")
    private UUID hospitalId;
}
