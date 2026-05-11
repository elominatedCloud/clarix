package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "clinical_notes")
public class ClinicalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(columnDefinition = "TEXT")
    private String subjective;

    @Column(columnDefinition = "TEXT")
    private String objective;

    @Column(columnDefinition = "TEXT")
    private String assessment;

    @Column(columnDefinition = "TEXT")
    private String plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NoteKind kind = NoteKind.SOAP;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getSubjective() { return subjective; }
    public void setSubjective(String subjective) { this.subjective = subjective; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getAssessment() { return assessment; }
    public void setAssessment(String assessment) { this.assessment = assessment; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public NoteKind getKind() { return kind; }
    public void setKind(NoteKind kind) { this.kind = kind; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
