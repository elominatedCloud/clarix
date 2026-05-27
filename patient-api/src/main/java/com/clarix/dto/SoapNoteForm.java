package com.clarix.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SoapNoteForm {
    @Size(max = 4000, message = "주관적 정보는 4000자 이하여야 합니다")
    private String subjective;

    @Size(max = 4000, message = "객관적 정보는 4000자 이하여야 합니다")
    private String objective;

    @Size(max = 4000, message = "평가는 4000자 이하여야 합니다")
    private String assessment;

    @Size(max = 4000, message = "계획은 4000자 이하여야 합니다")
    private String plan;
}
