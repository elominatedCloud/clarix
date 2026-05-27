package com.clarix.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoForm {
    @Size(max = 2000, message = "메모는 2000자 이하여야 합니다")
    private String memo;
}
