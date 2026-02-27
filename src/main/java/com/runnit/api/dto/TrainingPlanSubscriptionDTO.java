package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TrainingPlanSubscriptionDTO {
    private Long id;
    private Long planId;
    private String planTitle;
    private String planSportType;
    private int planDurationWeeks;
    private LocalDate startDate;
    private int currentWeek;
    private String status;
    private List<TrainingPlanWorkoutDTO> currentWeekWorkouts;
    private WeeklyPlanAdaptationDTO currentAdaptation;
    private Instant createdAt;
}
