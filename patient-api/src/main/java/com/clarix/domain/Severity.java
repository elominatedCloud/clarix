package com.clarix.domain;

public enum Severity {
    MINIMAL, MILD, MODERATE, MODERATELY_SEVERE, SEVERE;

    public static Severity fromScore(int score) {
        if (score <= 4)  return MINIMAL;
        if (score <= 9)  return MILD;
        if (score <= 14) return MODERATE;
        if (score <= 19) return MODERATELY_SEVERE;
        return SEVERE;
    }

    public String label() {
        return switch (this) {
            case MINIMAL -> "안정";
            case MILD -> "경미";
            case MODERATE -> "중등도";
            case MODERATELY_SEVERE -> "중등도-중증";
            case SEVERE -> "중증";
        };
    }
}
