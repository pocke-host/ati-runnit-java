package com.runnit.api.dto;

import java.time.LocalDateTime;

/** One session's aggregated numbers for a single exercise — the time-series unit for a progress chart. */
public class StrengthHistoryPoint {

    private final Long activityId;
    private final LocalDateTime performedAt;
    private final Double topWeightKg;
    private final Double totalVolumeKg;
    private final Integer totalReps;
    private final Integer setCount;
    private final Double estimatedOneRepMax;

    public StrengthHistoryPoint(Long activityId, LocalDateTime performedAt, Double topWeightKg,
                                 Double totalVolumeKg, Integer totalReps, Integer setCount,
                                 Double estimatedOneRepMax) {
        this.activityId = activityId;
        this.performedAt = performedAt;
        this.topWeightKg = topWeightKg;
        this.totalVolumeKg = totalVolumeKg;
        this.totalReps = totalReps;
        this.setCount = setCount;
        this.estimatedOneRepMax = estimatedOneRepMax;
    }

    public Long getActivityId() { return activityId; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public Double getTopWeightKg() { return topWeightKg; }
    public Double getTotalVolumeKg() { return totalVolumeKg; }
    public Integer getTotalReps() { return totalReps; }
    public Integer getSetCount() { return setCount; }
    public Double getEstimatedOneRepMax() { return estimatedOneRepMax; }
}
