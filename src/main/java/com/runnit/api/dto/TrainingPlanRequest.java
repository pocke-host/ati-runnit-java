package com.runnit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TrainingPlanRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String sportType;
    private String difficultyLevel = "BEGINNER";
    @NotNull
    private Integer durationWeeks;
    private boolean isAdaptive = true;
    private int priceCents = 0;
    private String tags; // comma-separated: recovery,brick,triathlon,ironman
    private String coverImageUrl;
    private boolean isPublished = false;
    private List<TrainingPlanWorkoutDTO> workouts;
}
