package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "medication_logs")
public class MedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MedStatus status;

    @Column(name = "taken_at", nullable = false)
    private OffsetDateTime takenAt;

    /** 환자 자가보고: 용량이 부족했는지 / 적당했는지 / 과했는지. nullable. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dosage_feedback", length = 16)
    private DosageFeedback dosageFeedback;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public MedStatus getStatus() { return status; }
    public void setStatus(MedStatus status) { this.status = status; }
    public OffsetDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(OffsetDateTime takenAt) { this.takenAt = takenAt; }
    public DosageFeedback getDosageFeedback() { return dosageFeedback; }
    public void setDosageFeedback(DosageFeedback dosageFeedback) { this.dosageFeedback = dosageFeedback; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
