package com.runnit.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class StrengthExerciseRequest {

    @NotBlank(message = "Exercise name is required")
    @Size(max = 120, message = "Exercise name cannot exceed 120 characters")
    private String exerciseName;

    /** Optional — server defaults to the exercise's index within the session if omitted. */
    private Integer sequenceOrder;

    private String notes;

    @NotEmpty(message = "At least one set is required")
    @Valid
    private List<StrengthSetRequest> sets;

    public StrengthExerciseRequest() {}

    public String getExerciseName() { return exerciseName; }
    public Integer getSequenceOrder() { return sequenceOrder; }
    public String getNotes() { return notes; }
    public List<StrengthSetRequest> getSets() { return sets; }

    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setSets(List<StrengthSetRequest> sets) { this.sets = sets; }
}
