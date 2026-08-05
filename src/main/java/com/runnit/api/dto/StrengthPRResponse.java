package com.runnit.api.dto;

import java.time.LocalDateTime;

/** Personal records for a single exercise, computed on-read from logged sets — nothing here is stored. */
public class StrengthPRResponse {

    /**
     * `value` is overloaded by context: kg lifted for heaviestWeight/bestRepsAtHeaviestWeight,
     * estimated 1RM in kg for estimatedOneRepMax, total session volume in kg for mostVolumeInSession.
     * `reps` is null for mostVolumeInSession, where a single rep count isn't meaningful.
     */
    public static class PRDetail {
        private final Double value;
        private final Integer reps;
        private final Long activityId;
        private final LocalDateTime performedAt;

        public PRDetail(Double value, Integer reps, Long activityId, LocalDateTime performedAt) {
            this.value = value;
            this.reps = reps;
            this.activityId = activityId;
            this.performedAt = performedAt;
        }

        public Double getValue() { return value; }
        public Integer getReps() { return reps; }
        public Long getActivityId() { return activityId; }
        public LocalDateTime getPerformedAt() { return performedAt; }
    }

    private final String exerciseName;
    private PRDetail heaviestWeight;
    private PRDetail bestRepsAtHeaviestWeight;
    private PRDetail estimatedOneRepMax;
    private PRDetail mostVolumeInSession;

    public StrengthPRResponse(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getExerciseName() { return exerciseName; }
    public PRDetail getHeaviestWeight() { return heaviestWeight; }
    public void setHeaviestWeight(PRDetail heaviestWeight) { this.heaviestWeight = heaviestWeight; }
    public PRDetail getBestRepsAtHeaviestWeight() { return bestRepsAtHeaviestWeight; }
    public void setBestRepsAtHeaviestWeight(PRDetail bestRepsAtHeaviestWeight) { this.bestRepsAtHeaviestWeight = bestRepsAtHeaviestWeight; }
    public PRDetail getEstimatedOneRepMax() { return estimatedOneRepMax; }
    public void setEstimatedOneRepMax(PRDetail estimatedOneRepMax) { this.estimatedOneRepMax = estimatedOneRepMax; }
    public PRDetail getMostVolumeInSession() { return mostVolumeInSession; }
    public void setMostVolumeInSession(PRDetail mostVolumeInSession) { this.mostVolumeInSession = mostVolumeInSession; }
}
