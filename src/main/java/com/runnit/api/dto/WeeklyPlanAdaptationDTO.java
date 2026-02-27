package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class WeeklyPlanAdaptationDTO {
    private Long id;
    private Long subscriptionId;
    private int weekNumber;
    private String adaptationNotes;
    private int volumeAdjustmentPercent;
    private String intensityAdjustment;
    private String aiReasoning;
    private Instant createdAt;
}
