package com.runnit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChallengeRequest {
    @NotBlank
    private String title;
    private String description;
    private String sportType;
    @NotBlank
    private String challengeType; // DISTANCE, DURATION, COUNT, STREAK
    @NotNull
    private Double goalValue;
    @NotBlank
    private String goalUnit; // KM, MILES, MINUTES, ACTIVITIES
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private boolean isGroup = false;
    private Integer maxParticipants;
    private boolean isPublic = true;
    private String charityName;
    private String charityUrl;
    private String coverImageUrl;
}
