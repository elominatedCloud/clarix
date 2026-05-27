package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 권한의 기준이 되는 역할입니다.
     * SecurityConfig의 URL 접근 제어와 화면 redirect가 이 값을 사용합니다.
     */
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
}
