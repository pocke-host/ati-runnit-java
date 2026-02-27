package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "training_plan_workouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingPlanWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TrainingPlan plan;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek; // 1=Monday, 7=Sunday

    // RECOVERY, BRICK, EASY_RUN, TEMPO, INTERVALS, LONG_RUN, REST, CROSS_TRAIN
    @Column(name = "workout_type", nullable = false)
    private String workoutType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_duration_minutes")
    private Integer targetDurationMinutes;

    @Column(name = "target_distance_meters")
    private Integer targetDistanceMeters;

    @Column(name = "target_heart_rate_zone")
    private Integer targetHeartRateZone; // 1-5

    @Column(nullable = false)
    private String intensity = "MODERATE"; // EASY, MODERATE, HARD, MAX

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
