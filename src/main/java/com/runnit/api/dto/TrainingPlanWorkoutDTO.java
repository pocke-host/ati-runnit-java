package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TrainingPlanWorkoutDTO {
    private Long id;
    private Long planId;
    private int weekNumber;
    private int dayOfWeek;
    private String workoutType;
    private String title;
    private String description;
    private Integer targetDurationMinutes;
    private Integer targetDistanceMeters;
    private Integer targetHeartRateZone;
    private String intensity;
    private String notes;
    private Instant createdAt;
}
