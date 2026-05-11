package com.clarix.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "symptom_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "log_date"}))
public class SymptomLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(name = "mood_score")
    private Integer moodScore;  // 1..5

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", length = 16)
    private Emotion emotion;

    /** 부작용 체크리스트를 JSON 문자열로 저장 (H2/Postgres 공통). 예: {"두통":true,"불면":true} */
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "journal", columnDefinition = "TEXT")
    private String journal;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public Integer getMoodScore() { return moodScore; }
    public void setMoodScore(Integer moodScore) { this.moodScore = moodScore; }
    public Emotion getEmotion() { return emotion; }
    public void setEmotion(Emotion emotion) { this.emotion = emotion; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getJournal() { return journal; }
    public void setJournal(String journal) { this.journal = journal; }
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
