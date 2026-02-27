package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Challenge {

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

    @Column(name = "sport_type")
    private String sportType;

    // DISTANCE, DURATION, COUNT, STREAK
    @Column(name = "challenge_type", nullable = false)
    private String challengeType;

    @Column(name = "goal_value", nullable = false)
    private double goalValue;

    // KM, MILES, MINUTES, ACTIVITIES
    @Column(name = "goal_unit", nullable = false)
    private String goalUnit;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_group", nullable = false)
    private boolean isGroup = false;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @Column(name = "charity_name")
    private String charityName;

    @Column(name = "charity_url", length = 1000)
    private String charityUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
