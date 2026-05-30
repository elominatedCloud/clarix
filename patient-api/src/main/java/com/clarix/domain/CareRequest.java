package com.clarix.domain;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_requests")
public class CareRequest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DISPLAY =
        DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareRequestStatus status = CareRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "handled_at")
    private OffsetDateTime handledAt;

    public boolean isPending() {
        return status == CareRequestStatus.PENDING;
    }

    public String createdAtDisplay() {
        return createdAt.atZoneSameInstant(CLINIC_ZONE).format(DISPLAY);
    }

    public String statusLabel() {
        return status.label();
    }

    public String statusTone() {
        return switch (status) {
            case PENDING -> "warn";
            case SCHEDULED -> "ok";
            case CANCELED -> "muted";
        };
    }
}
