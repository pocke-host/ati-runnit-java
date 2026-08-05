package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "strength_exercises")
public class StrengthExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "exercise_name", nullable = false, length = 120)
    private String exerciseName;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 0;

    @Column(name = "notes", length = 255)
    private String notes;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("setNumber ASC")
    private List<StrengthSet> sets = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StrengthExercise() {}

    public Long getId() { return id; }
    public Activity getActivity() { return activity; }
    public String getExerciseName() { return exerciseName; }
    public Integer getSequenceOrder() { return sequenceOrder; }
    public String getNotes() { return notes; }
    public List<StrengthSet> getSets() { return sets; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setSets(List<StrengthSet> sets) { this.sets = sets; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Activity activity;
        private String exerciseName;
        private Integer sequenceOrder = 0;
        private String notes;

        public Builder activity(Activity activity) { this.activity = activity; return this; }
        public Builder exerciseName(String exerciseName) { this.exerciseName = exerciseName; return this; }
        public Builder sequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }

        public StrengthExercise build() {
            StrengthExercise e = new StrengthExercise();
            e.activity = this.activity;
            e.exerciseName = this.exerciseName;
            e.sequenceOrder = this.sequenceOrder != null ? this.sequenceOrder : 0;
            e.notes = this.notes;
            return e;
        }
    }
}
