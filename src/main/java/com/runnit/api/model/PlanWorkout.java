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

    public PlanWorkout() {}

    public Long getId() { return id; }
    public Plan getPlan() { return plan; }
    public Integer getDay() { return day; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public boolean isCompleted() { return completed; }

    public void setId(Long id) { this.id = id; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public void setDay(Integer day) { this.day = day; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Plan plan;
        private Integer day;
        private String title;
        private String description;
        private Integer durationMinutes;
        private Integer distanceMeters;
        private boolean completed = false;

        public Builder plan(Plan plan) { this.plan = plan; return this; }
        public Builder day(Integer day) { this.day = day; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder durationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public Builder distanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; return this; }
        public Builder completed(boolean completed) { this.completed = completed; return this; }

        public PlanWorkout build() {
            PlanWorkout w = new PlanWorkout();
            w.plan = this.plan;
            w.day = this.day;
            w.title = this.title;
            w.description = this.description;
            w.durationMinutes = this.durationMinutes;
            w.distanceMeters = this.distanceMeters;
            w.completed = this.completed;
            return w;
        }
    }
}
