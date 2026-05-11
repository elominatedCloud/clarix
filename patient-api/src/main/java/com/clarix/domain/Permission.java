package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "doctor_id"}))
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt = OffsetDateTime.now();

    /** 의사가 마지막으로 환자 상세를 연 시각. 환자가 권한을 부여한 의사가 실제로 보고 있는지 확인용. */
    @Column(name = "last_viewed_at")
    private OffsetDateTime lastViewedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public OffsetDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(OffsetDateTime grantedAt) { this.grantedAt = grantedAt; }
    public OffsetDateTime getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(OffsetDateTime lastViewedAt) { this.lastViewedAt = lastViewedAt; }
}
