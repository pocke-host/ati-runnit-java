package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit row for a single field changed on a PlanWorkout by the adaptive plan
 * engine — one row per (workout, field) so old/new values stay atomic per
 * column. No FK constraints (PlanetScale doesn't support them), so ids are
 * plain Longs, matching WellnessDaily's style.
 */
@Entity
@Table(name = "plan_adaptations")
public class PlanAdaptation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "plan_workout_id", nullable = false)
    private Long planWorkoutId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** "ACTIVITY" (fired by a completed workout) or "SCHEDULED" (nightly sweep). */
    @Column(name = "triggered_by", nullable = false, length = 20)
    private String triggeredBy;

    @Column(name = "trigger_activity_id")
    private Long triggerActivityId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** Which PlanWorkout field changed: "workoutType", "targetPaceSeconds", "durationMinutes", "distanceMeters". */
    @Column(name = "field_changed", nullable = false, length = 50)
    private String fieldChanged;

    @Column(name = "old_value", length = 100)
    private String oldValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PlanAdaptation() {}

    public Long getId() { return id; }
    public Long getPlanId() { return planId; }
    public Long getPlanWorkoutId() { return planWorkoutId; }
    public Long getUserId() { return userId; }
    public String getTriggeredBy() { return triggeredBy; }
    public Long getTriggerActivityId() { return triggerActivityId; }
    public String getReason() { return reason; }
    public String getFieldChanged() { return fieldChanged; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public void setPlanWorkoutId(Long planWorkoutId) { this.planWorkoutId = planWorkoutId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public void setTriggerActivityId(Long triggerActivityId) { this.triggerActivityId = triggerActivityId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setFieldChanged(String fieldChanged) { this.fieldChanged = fieldChanged; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long planId;
        private Long planWorkoutId;
        private Long userId;
        private String triggeredBy;
        private Long triggerActivityId;
        private String reason;
        private String fieldChanged;
        private String oldValue;
        private String newValue;

        public Builder planId(Long v) { this.planId = v; return this; }
        public Builder planWorkoutId(Long v) { this.planWorkoutId = v; return this; }
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder triggeredBy(String v) { this.triggeredBy = v; return this; }
        public Builder triggerActivityId(Long v) { this.triggerActivityId = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder fieldChanged(String v) { this.fieldChanged = v; return this; }
        public Builder oldValue(String v) { this.oldValue = v; return this; }
        public Builder newValue(String v) { this.newValue = v; return this; }

        public PlanAdaptation build() {
            PlanAdaptation a = new PlanAdaptation();
            a.planId = this.planId;
            a.planWorkoutId = this.planWorkoutId;
            a.userId = this.userId;
            a.triggeredBy = this.triggeredBy;
            a.triggerActivityId = this.triggerActivityId;
            a.reason = this.reason;
            a.fieldChanged = this.fieldChanged;
            a.oldValue = this.oldValue;
            a.newValue = this.newValue;
            return a;
        }
    }
}
