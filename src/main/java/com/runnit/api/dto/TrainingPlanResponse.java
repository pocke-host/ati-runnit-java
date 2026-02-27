package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TrainingPlanResponse {
    private Long id;
    private Long creatorId;
    private String creatorDisplayName;
    private boolean creatorVerified;
    private String title;
    private String description;
    private String sportType;
    private String difficultyLevel;
    private int durationWeeks;
    private boolean isAdaptive;
    private boolean isVerified;
    private int priceCents;
    private String tags;
    private String coverImageUrl;
    private boolean isPublished;
    private int subscriberCount;
    private boolean isSubscribed;
    private List<TrainingPlanWorkoutDTO> workouts;
    private Instant createdAt;
}
