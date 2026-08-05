package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "strength_sets")
public class StrengthSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strength_exercise_id", nullable = false)
    private StrengthExercise exercise;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(name = "reps", nullable = false)
    private Integer reps;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "is_warmup", nullable = false)
    private Boolean isWarmup = false;

    @Column(name = "rpe")
    private Double rpe;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StrengthSet() {}

    public Long getId() { return id; }
    public StrengthExercise getExercise() { return exercise; }
    public Integer getSetNumber() { return setNumber; }
    public Integer getReps() { return reps; }
    public Double getWeightKg() { return weightKg; }
    public Boolean getIsWarmup() { return isWarmup; }
    public Double getRpe() { return rpe; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setExercise(StrengthExercise exercise) { this.exercise = exercise; }
    public void setSetNumber(Integer setNumber) { this.setNumber = setNumber; }
    public void setReps(Integer reps) { this.reps = reps; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public void setIsWarmup(Boolean isWarmup) { this.isWarmup = isWarmup; }
    public void setRpe(Double rpe) { this.rpe = rpe; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private StrengthExercise exercise;
        private Integer setNumber;
        private Integer reps;
        private Double weightKg;
        private Boolean isWarmup = false;
        private Double rpe;

        public Builder exercise(StrengthExercise exercise) { this.exercise = exercise; return this; }
        public Builder setNumber(Integer setNumber) { this.setNumber = setNumber; return this; }
        public Builder reps(Integer reps) { this.reps = reps; return this; }
        public Builder weightKg(Double weightKg) { this.weightKg = weightKg; return this; }
        public Builder isWarmup(Boolean isWarmup) { this.isWarmup = isWarmup; return this; }
        public Builder rpe(Double rpe) { this.rpe = rpe; return this; }

        public StrengthSet build() {
            StrengthSet s = new StrengthSet();
            s.exercise = this.exercise;
            s.setNumber = this.setNumber;
            s.reps = this.reps;
            s.weightKg = this.weightKg;
            s.isWarmup = this.isWarmup != null ? this.isWarmup : false;
            s.rpe = this.rpe;
            return s;
        }
    }
}
