package com.clarix.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareGrantForm {
    @NotNull(message = "공유할 의사를 선택하세요")
    private UUID doctorId;
}
