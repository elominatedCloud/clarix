package com.clarix.dto;

import lombok.Data;

@Data
public class SignupForm {
    private String email;
    private String password;
    private String name;
    private String role;
}
