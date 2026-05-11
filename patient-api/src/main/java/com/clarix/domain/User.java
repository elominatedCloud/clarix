package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    /** 12주차 Spring Security 도입 전까지는 평문/단순 해시. 학습용. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    /** 데스크 직원이 환자별로 남기는 예약/연락 메모. (env: 환자 본인은 못 봄) */
    @Column(name = "reception_memo", columnDefinition = "TEXT")
    private String receptionMemo;

    /** 의사가 환자에게 남기는 진단/관찰 메모. (환자 본인은 못 봄) */
    @Column(name = "doctor_memo", columnDefinition = "TEXT")
    private String doctorMemo;

    /** 다음 진료 예약이 잡힌 시각. null이면 미예약. */
    @Column(name = "booked_at")
    private OffsetDateTime bookedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Hospital getHospital() { return hospital; }
    public void setHospital(Hospital hospital) { this.hospital = hospital; }
    public String getReceptionMemo() { return receptionMemo; }
    public void setReceptionMemo(String receptionMemo) { this.receptionMemo = receptionMemo; }
    public String getDoctorMemo() { return doctorMemo; }
    public void setDoctorMemo(String doctorMemo) { this.doctorMemo = doctorMemo; }
    public OffsetDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(OffsetDateTime bookedAt) { this.bookedAt = bookedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
