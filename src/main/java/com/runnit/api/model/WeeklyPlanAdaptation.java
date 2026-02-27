package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "weekly_plan_adaptations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyPlanAdaptation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private TrainingPlanSubscription subscription;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "adaptation_notes", columnDefinition = "TEXT")
    private String adaptationNotes;

    @Column(name = "volume_adjustment_percent", nullable = false)
    private int volumeAdjustmentPercent = 0;

    @Column(name = "intensity_adjustment")
    private String intensityAdjustment;

    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
