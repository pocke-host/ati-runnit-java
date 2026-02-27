package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "training_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sport_type", nullable = false)
    private String sportType = "RUN";

    @Column(name = "difficulty_level", nullable = false)
    private String difficultyLevel = "BEGINNER"; // BEGINNER, INTERMEDIATE, ADVANCED, ELITE

    @Column(name = "duration_weeks", nullable = false)
    private int durationWeeks = 12;

    @Column(name = "is_adaptive", nullable = false)
    private boolean isAdaptive = true;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "price_cents", nullable = false)
    private int priceCents = 0;

    @Column(columnDefinition = "VARCHAR(1000)")
    private String tags; // comma-separated: recovery,brick,triathlon,ironman

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    @Column(name = "subscriber_count", nullable = false)
    private int subscriberCount = 0;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("weekNumber ASC, dayOfWeek ASC")
    private List<TrainingPlanWorkout> workouts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
