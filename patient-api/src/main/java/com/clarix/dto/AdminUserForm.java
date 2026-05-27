package com.clarix.dto;

import com.clarix.domain.Role;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserForm {
    @NotNull(message = "역할을 선택하세요")
    private Role role;

    @Size(max = 36, message = "병원 ID가 올바르지 않습니다")
    @Pattern(regexp = "|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        message = "병원 ID가 올바르지 않습니다")
    private String hospitalId;
}
