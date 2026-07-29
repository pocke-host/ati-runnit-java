package com.runnit.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "plan_workouts")
public class PlanWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "day", nullable = false)
    private Integer day;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    @Column(name = "workout_type", length = 50)
    private String workoutType;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "target_pace_seconds")
    private Integer targetPaceSeconds;

    @Column(name = "linked_activity_id")
    private Long linkedActivityId;

    @Column(name = "target_heart_rate")
    private Integer targetHeartRate;

    @Column(name = "target_rpe")
    private Integer targetRpe;

    @Column(name = "adapted_at")
    private java.time.LocalDateTime adaptedAt;

    @Column(name = "original_target_pace_seconds")
    private Integer originalTargetPaceSeconds;

    @Column(name = "original_duration_minutes")
    private Integer originalDurationMinutes;

    @Column(name = "original_distance_meters")
    private Integer originalDistanceMeters;

    @Column(name = "original_workout_type", length = 50)
    private String originalWorkoutType;

    public PlanWorkout() {}

    public Long getId() { return id; }
    public Plan getPlan() { return plan; }
    public Integer getDay() { return day; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public boolean isCompleted() { return completed; }
    public String getWorkoutType() { return workoutType; }
    public Integer getWeekNumber() { return weekNumber; }
    public Integer getTargetPaceSeconds() { return targetPaceSeconds; }
    public Long getLinkedActivityId() { return linkedActivityId; }
    public Integer getTargetHeartRate() { return targetHeartRate; }
    public Integer getTargetRpe() { return targetRpe; }
    public java.time.LocalDateTime getAdaptedAt() { return adaptedAt; }
    public Integer getOriginalTargetPaceSeconds() { return originalTargetPaceSeconds; }
    public Integer getOriginalDurationMinutes() { return originalDurationMinutes; }
    public Integer getOriginalDistanceMeters() { return originalDistanceMeters; }
    public String getOriginalWorkoutType() { return originalWorkoutType; }

    public void setId(Long id) { this.id = id; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public void setDay(Integer day) { this.day = day; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public void setTargetPaceSeconds(Integer targetPaceSeconds) { this.targetPaceSeconds = targetPaceSeconds; }
    public void setLinkedActivityId(Long linkedActivityId) { this.linkedActivityId = linkedActivityId; }
    public void setTargetHeartRate(Integer targetHeartRate) { this.targetHeartRate = targetHeartRate; }
    public void setTargetRpe(Integer targetRpe) { this.targetRpe = targetRpe; }
    public void setAdaptedAt(java.time.LocalDateTime adaptedAt) { this.adaptedAt = adaptedAt; }
    public void setOriginalTargetPaceSeconds(Integer v) { this.originalTargetPaceSeconds = v; }
    public void setOriginalDurationMinutes(Integer v) { this.originalDurationMinutes = v; }
    public void setOriginalDistanceMeters(Integer v) { this.originalDistanceMeters = v; }
    public void setOriginalWorkoutType(String v) { this.originalWorkoutType = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Plan plan;
        private Integer day;
        private String title;
        private String description;
        private Integer durationMinutes;
        private Integer distanceMeters;
        private boolean completed = false;
        private String workoutType;
        private Integer weekNumber;
        private Integer targetPaceSeconds;

        public Builder plan(Plan plan) { this.plan = plan; return this; }
        public Builder day(Integer day) { this.day = day; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder durationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public Builder distanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; return this; }
        public Builder completed(boolean completed) { this.completed = completed; return this; }
        public Builder workoutType(String workoutType) { this.workoutType = workoutType; return this; }
        public Builder weekNumber(Integer weekNumber) { this.weekNumber = weekNumber; return this; }
        public Builder targetPaceSeconds(Integer targetPaceSeconds) { this.targetPaceSeconds = targetPaceSeconds; return this; }

        public PlanWorkout build() {
            PlanWorkout w = new PlanWorkout();
            w.plan = this.plan;
            w.day = this.day;
            w.title = this.title;
            w.description = this.description;
            w.durationMinutes = this.durationMinutes;
            w.distanceMeters = this.distanceMeters;
            w.completed = this.completed;
            w.workoutType = this.workoutType;
            w.weekNumber = this.weekNumber;
            w.targetPaceSeconds = this.targetPaceSeconds;
            return w;
        }
    }
}
