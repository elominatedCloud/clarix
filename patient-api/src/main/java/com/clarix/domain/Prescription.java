package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 처방. 매일 복약 토글이 이 목록을 순회.
 * schedule는 "morning,noon,evening" 부분집합을 쉼표 구분 문자열로 저장.
 * MySQL VARCHAR 컬럼에 String으로 저장하고 헬퍼에서 List로 변환한다.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(nullable = false, length = 64)
    private String schedule;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "days_supply", nullable = false)
    private int daysSupply = 30;

    /** 약을 어디에 보관하는지 — 환자 본인의 메모 (예: 침실 협탁, 부엌 서랍, 냉장고 등). */
    @Column(name = "storage_location", length = 200)
    private String storageLocation;

    /** 약 보관 위치 사진 URL (v2 — 업로드 후 경로). */
    @Column(name = "storage_photo_url", length = 500)
    private String storagePhotoUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public List<String> scheduleSlots() {
        if (schedule == null || schedule.isBlank()) return List.of();
        return Arrays.stream(schedule.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
    public void setScheduleSlots(List<String> slots) {
        this.schedule = slots == null ? "" : String.join(",", slots);
    }

    /** 처방 종료일 (createdAt + daysSupply). */
    public java.time.LocalDate endDate() {
        return createdAt.toLocalDate().plusDays(daysSupply);
    }
}
