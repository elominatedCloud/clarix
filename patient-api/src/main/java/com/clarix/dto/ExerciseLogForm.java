package com.clarix.dto;

import lombok.Data;

@Data
public class ExerciseLogForm {
    private String kind;
    private int durationMin;
    private String note;
}
