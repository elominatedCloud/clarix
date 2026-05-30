package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 수면 시간 기록. 환자가 직접 입력하거나 향후 Apple 건강/삼성 헬스 연동 데이터를 받아 저장.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sleep_logs")
public class SleepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    /** 수면 시간(시간 단위, 소수점 허용). 0 ~ 24. */
    @Column(name = "hours", nullable = false)
    private double hours;

    /** 데이터 출처: MANUAL, APPLE_HEALTH, SAMSUNG_HEALTH */
    @Column(name = "source", length = 32, nullable = false)
    private String source = "MANUAL";

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt = OffsetDateTime.now();
}
