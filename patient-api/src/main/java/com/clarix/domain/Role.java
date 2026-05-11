package com.clarix.domain;

public enum Role {
    PATIENT,
    DOCTOR,
    NURSE,
    TECHNICIAN,
    RECEPTIONIST,
    ADMIN;

    public String label() {
        return switch (this) {
            case PATIENT      -> "환자";
            case DOCTOR       -> "의사";
            case NURSE        -> "간호사";
            case TECHNICIAN   -> "의료기사";
            case RECEPTIONIST -> "데스크";
            case ADMIN        -> "관리자";
        };
    }

    /** 의사 본인이 발급할 수 있는 staff 역할. */
    public boolean isStaffInvitable() {
        return this == NURSE || this == TECHNICIAN || this == RECEPTIONIST;
    }
}
