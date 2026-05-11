package com.clarix.domain;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

/**
 * 처방. 매일 복약 토글이 이 목록을 순회.
 * schedule는 "morning,noon,evening" 부분집합을 쉼표 구분 문자열로 저장.
 * (TEXT[]는 H2/Postgres 호환이 까다로워 String + 헬퍼로 단순화)
 */
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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDaysSupply() { return daysSupply; }
    public void setDaysSupply(int daysSupply) { this.daysSupply = daysSupply; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public String getStoragePhotoUrl() { return storagePhotoUrl; }
    public void setStoragePhotoUrl(String storagePhotoUrl) { this.storagePhotoUrl = storagePhotoUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    /** 처방 종료일 (createdAt + daysSupply). */
    public java.time.LocalDate endDate() {
        return createdAt.toLocalDate().plusDays(daysSupply);
    }
}
