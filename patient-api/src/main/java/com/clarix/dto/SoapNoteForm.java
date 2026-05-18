package com.clarix.dto;

import lombok.Data;

@Data
public class SoapNoteForm {
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
}
