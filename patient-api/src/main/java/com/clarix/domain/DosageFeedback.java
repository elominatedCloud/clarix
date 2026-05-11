package com.clarix.domain;

public enum DosageFeedback {
    TOO_LOW, OK, TOO_HIGH;

    public String label() {
        return switch (this) {
            case TOO_LOW  -> "부족";
            case OK       -> "적당";
            case TOO_HIGH -> "과다";
        };
    }
}
