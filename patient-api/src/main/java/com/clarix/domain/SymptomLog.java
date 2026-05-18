package com.clarix.domain;

import java.time.LocalDate;
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
@Table(name = "symptom_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "log_date"}))
public class SymptomLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(name = "mood_score")
    private Integer moodScore;  // 1..5

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", length = 16)
    private Emotion emotion;

    /** 부작용 체크리스트를 JSON 문자열로 저장. 예: {"두통":true,"불면":true} */
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "journal", columnDefinition = "TEXT")
    private String journal;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
