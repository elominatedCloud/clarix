package com.clarix.dto;

import com.clarix.domain.Emotion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MoodEntryForm {
    @NotNull(message = "감정을 선택하세요")
    private Emotion emotion;

    @Size(max = 2000, message = "기록은 2000자 이하여야 합니다")
    private String journal;
}
