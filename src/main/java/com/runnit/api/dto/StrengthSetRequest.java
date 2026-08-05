package com.runnit.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StrengthSetRequest {

    /** Optional — server defaults to the set's index+1 within its exercise if omitted. */
    private Integer setNumber;

    @NotNull(message = "Reps are required")
    @Min(value = 0, message = "Reps cannot be negative")
    private Integer reps;

    /** Nullable — bodyweight movements (pull-ups, dips, planks) have no weight. */
    private Double weightKg;

    private Boolean isWarmup = false;

    @DecimalMin(value = "1.0", message = "RPE must be at least 1")
    @DecimalMax(value = "10.0", message = "RPE cannot exceed 10")
    private Double rpe;

    public StrengthSetRequest() {}

    public Integer getSetNumber() { return setNumber; }
    public Integer getReps() { return reps; }
    public Double getWeightKg() { return weightKg; }
    public Boolean getIsWarmup() { return isWarmup; }
    public Double getRpe() { return rpe; }

    public void setSetNumber(Integer setNumber) { this.setNumber = setNumber; }
    public void setReps(Integer reps) { this.reps = reps; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public void setIsWarmup(Boolean isWarmup) { this.isWarmup = isWarmup; }
    public void setRpe(Double rpe) { this.rpe = rpe; }
}
