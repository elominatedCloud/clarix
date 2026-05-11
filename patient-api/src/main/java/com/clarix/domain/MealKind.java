package com.clarix.domain;

public enum MealKind {
    BREAKFAST("아침", "🍚"),
    LUNCH    ("점심", "🍱"),
    DINNER   ("저녁", "🍽️"),
    FRUIT    ("과일", "🍎"),
    DESSERT  ("디저트", "🍰");

    private final String label;
    private final String emoji;

    MealKind(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }

    public String label() { return label; }
    public String emoji() { return emoji; }
}
