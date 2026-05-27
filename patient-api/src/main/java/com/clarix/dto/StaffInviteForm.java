package com.clarix.dto;

import com.clarix.domain.Role;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffInviteForm {
    @NotBlank(message = "이메일을 입력하세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotNull(message = "역할을 선택하세요")
    private Role role;

    @AssertTrue(message = "초대 가능한 역할이 아닙니다")
    public boolean isInvitableRole() {
        return role == null || role.isStaffInvitable();
    }
}
