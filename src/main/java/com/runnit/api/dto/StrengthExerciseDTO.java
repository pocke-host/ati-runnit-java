package com.runnit.api.dto;

import com.runnit.api.model.StrengthExercise;
import com.runnit.api.model.StrengthSet;

import java.util.List;
import java.util.stream.Collectors;

/** One exercise within a STRENGTH activity's detail view, with its full set list. */
public class StrengthExerciseDTO {

    private Long id;
    private String exerciseName;
    private Integer sequenceOrder;
    private String notes;
    private List<StrengthSetDetail> sets;

    public static StrengthExerciseDTO from(StrengthExercise exercise, List<StrengthSet> sets) {
        StrengthExerciseDTO dto = new StrengthExerciseDTO();
        dto.id = exercise.getId();
        dto.exerciseName = exercise.getExerciseName();
        dto.sequenceOrder = exercise.getSequenceOrder();
        dto.notes = exercise.getNotes();
        dto.sets = sets.stream().map(StrengthSetDetail::from).collect(Collectors.toList());
        return dto;
    }

    public Long getId() { return id; }
    public String getExerciseName() { return exerciseName; }
    public Integer getSequenceOrder() { return sequenceOrder; }
    public String getNotes() { return notes; }
    public List<StrengthSetDetail> getSets() { return sets; }
}
