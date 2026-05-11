package com.clarix.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionForm {
    private String medicationName;
    private List<String> slots = new ArrayList<>();
    private int daysSupply = 30;
}
