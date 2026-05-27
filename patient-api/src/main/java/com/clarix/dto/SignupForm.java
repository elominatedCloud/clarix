package com.clarix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupForm {
    // Bean Validation은 Controller의 @Valid가 실행될 때 자동 검사됩니다.
    // 실패하면 BindingResult에 에러가 담기고, Service는 호출하지 않습니다.
    @NotBlank(message = "이메일을 입력하세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요")
    @Size(min = 6, max = 72, message = "비밀번호는 6자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "이름을 입력하세요")
    @Size(max = 60, message = "이름은 60자 이하여야 합니다")
    private String name;

    @NotBlank(message = "가입 유형을 선택하세요")
    @Pattern(regexp = "patient|doctor", message = "가입 유형이 올바르지 않습니다")
    private String role;
}
