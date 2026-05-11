package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "meal_logs")
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MealKind kind;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public MealKind getKind() { return kind; }
    public void setKind(MealKind kind) { this.kind = kind; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OffsetDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(OffsetDateTime loggedAt) { this.loggedAt = loggedAt; }
}
