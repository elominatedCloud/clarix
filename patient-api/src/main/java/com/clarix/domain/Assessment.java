package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(nullable = false, length = 20)
    private String kind = "PHQ-9";

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Severity severity;

    /** 답변 9개를 JSON 문자열로 저장. 예: {"q1":2,"q2":1,...} */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answers;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public Hospital getHospital() { return hospital; }
    public void setHospital(Hospital hospital) { this.hospital = hospital; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
