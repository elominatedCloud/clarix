package com.clarix.dto;

import lombok.Data;

@Data
public class HospitalForm {
    private String name;
    private String address;
    private String specialty;
    private boolean partnered = true;
}
