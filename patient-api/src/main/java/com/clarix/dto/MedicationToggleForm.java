package com.clarix.dto;

import lombok.Data;

@Data
public class MedicationToggleForm {
    private String medicationName;
    private String slot;
    private String status;
    private String returnTo = "/patient/";
}
