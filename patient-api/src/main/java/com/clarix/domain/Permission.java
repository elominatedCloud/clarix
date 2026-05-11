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
}
