package com.clarix.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class DosageFeedbackForm {
    private UUID logId;
    private String feedback;
}
