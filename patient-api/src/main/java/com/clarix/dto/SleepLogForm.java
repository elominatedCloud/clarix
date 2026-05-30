package com.clarix.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

// 수면 시간 직접 입력 폼. 환자가 모바일 화면에서 시간을 적어 보낼 때 바인딩됨.
@Getter
@Setter
public class SleepLogForm {

    @DecimalMin(value = "0.0", message = "수면 시간은 0 이상이어야 합니다.")
    @DecimalMax(value = "24.0", message = "수면 시간은 24 이하이어야 합니다.")
    private double hours;
}
