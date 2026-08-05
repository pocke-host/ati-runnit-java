package com.runnit.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class StrengthActivityRequest {

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    private Integer calories;
    private String notes;

    @NotEmpty(message = "At least one exercise is required")
    @Valid
    private List<StrengthExerciseRequest> exercises;

    public StrengthActivityRequest() {}

    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getCalories() { return calories; }
    public String getNotes() { return notes; }
    public List<StrengthExerciseRequest> getExercises() { return exercises; }

    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setExercises(List<StrengthExerciseRequest> exercises) { this.exercises = exercises; }
}
