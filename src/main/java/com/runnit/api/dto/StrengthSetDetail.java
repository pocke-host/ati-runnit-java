package com.runnit.api.dto;

import com.runnit.api.model.StrengthSet;

/** Flat set detail nested under StrengthExerciseDTO — one row per logged set. */
public class StrengthSetDetail {

    private Long id;
    private Integer setNumber;
    private Integer reps;
    private Double weightKg;
    private Boolean isWarmup;
    private Double rpe;

    public static StrengthSetDetail from(StrengthSet s) {
        StrengthSetDetail d = new StrengthSetDetail();
        d.id = s.getId();
        d.setNumber = s.getSetNumber();
        d.reps = s.getReps();
        d.weightKg = s.getWeightKg();
        d.isWarmup = s.getIsWarmup();
        d.rpe = s.getRpe();
        return d;
    }

    public Long getId() { return id; }
    public Integer getSetNumber() { return setNumber; }
    public Integer getReps() { return reps; }
    public Double getWeightKg() { return weightKg; }
    public Boolean getIsWarmup() { return isWarmup; }
    public Double getRpe() { return rpe; }
}
