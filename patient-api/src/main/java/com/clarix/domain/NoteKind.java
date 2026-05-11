package com.clarix.domain;

public enum NoteKind {
    SOAP,        // 의사의 임상 노트
    NURSING,     // 간호 노트
    TECH;        // 의료기사 노트

    public String label() {
        return switch (this) {
            case SOAP    -> "SOAP";
            case NURSING -> "간호";
            case TECH    -> "의료기사";
        };
    }

    public static NoteKind forRole(Role role) {
        if (role == null) return SOAP;
        return switch (role) {
            case NURSE      -> NURSING;
            case TECHNICIAN -> TECH;
            default         -> SOAP;
        };
    }
}
