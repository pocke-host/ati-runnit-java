package com.runnit.api.dto;

/**
 * Response DTO for GET /api/plans/active/adaptations.
 * One entry per PlanAdaptation audit row — a single field change on a single
 * plan workout, with a human-readable explanation of why it happened.
 */
public class AdaptationResponse {

    private Long id;

    /** ID of the PlanWorkout this adaptation applied to. */
    private Long planWorkoutId;

    /** "ACTIVITY" (fired by a completed workout) or "SCHEDULED" (nightly sweep). */
    private String triggeredBy;

    /** Activity ID that triggered this adaptation, if triggeredBy == "ACTIVITY". Null otherwise. */
    private Long triggerActivityId;

    /** Human-readable explanation, e.g. "Your ACWR is 1.62 (>1.5, high injury risk)..." */
    private String reason;

    /** Which PlanWorkout field changed: "workoutType", "targetPaceSeconds", "durationMinutes", "distanceMeters". */
    private String fieldChanged;

    private String oldValue;
    private String newValue;
    private String createdAt;

    public AdaptationResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanWorkoutId() { return planWorkoutId; }
    public void setPlanWorkoutId(Long planWorkoutId) { this.planWorkoutId = planWorkoutId; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public Long getTriggerActivityId() { return triggerActivityId; }
    public void setTriggerActivityId(Long triggerActivityId) { this.triggerActivityId = triggerActivityId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getFieldChanged() { return fieldChanged; }
    public void setFieldChanged(String fieldChanged) { this.fieldChanged = fieldChanged; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
