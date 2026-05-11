package com.clarix.domain;

public enum Emotion {
    JOY(5, "기쁨", "joy", "😃"),
    CALM(4, "평온", "calm", "😌"),
    TIRED(2, "피곤", "tired", "😪"),
    SAD(2, "슬픔", "sad", "😢"),
    DOWN(1, "우울", "down", "😔"),
    ANGRY(1, "짜증", "angry", "😠");

    private final int score;
    private final String label;
    private final String slug;
    private final String emoji;

    Emotion(int score, String label, String slug, String emoji) {
        this.score = score;
        this.label = label;
        this.slug = slug;
        this.emoji = emoji;
    }

    public int score() { return score; }
    public String label() { return label; }
    public String slug() { return slug; }
    public String emoji() { return emoji; }
}
