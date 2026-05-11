package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "exercise_logs")
public class ExerciseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    /** 자유 텍스트 (걷기, 헬스, 요가, ...) — 환자가 직접 추가 가능 */
    @Column(nullable = false, length = 64)
    private String kind;

    /** 소요 시간(분). 0 이상. */
    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OffsetDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(OffsetDateTime loggedAt) { this.loggedAt = loggedAt; }
}
