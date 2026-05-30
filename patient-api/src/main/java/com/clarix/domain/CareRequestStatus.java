package com.clarix.domain;

public enum CareRequestStatus {
    PENDING("요청 접수"),
    SCHEDULED("예약 안내 완료"),
    CANCELED("요청 취소");

    private final String label;

    CareRequestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
