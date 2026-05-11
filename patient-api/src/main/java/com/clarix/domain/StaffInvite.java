package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 의사가 같은 병원 내 staff(간호사/데스크)에게 발급하는 초대.
 * staff가 동일 이메일로 회원가입할 때 자동으로 role + hospital + doctor가 적용된다.
 *
 *   - active=false면 가입 시 매칭되지 않음 (즉 신규 가입 차단)
 *   - 이미 가입된 staff의 활성/비활성은 User.active 등 다른 메커니즘 (단계 1에선 invite.active로 신규 차단만)
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "staff_invites",
       uniqueConstraints = @UniqueConstraint(columnNames = {"email", "doctor_id"}))
public class StaffInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
