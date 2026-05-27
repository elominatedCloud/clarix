package com.clarix.dto;

import java.util.UUID;

import com.clarix.domain.DosageFeedback;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DosageFeedbackForm {
    @NotNull(message = "복약 기록이 비어 있습니다")
    private UUID logId;

    @NotNull(message = "복약 피드백을 선택하세요")
    private DosageFeedback feedback;
}
