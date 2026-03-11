package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "workout_events")
public class WorkoutEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "workout_type", length = 50)
    private String workoutType;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "target_pace_seconds")
    private Integer targetPaceSeconds;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "source", length = 50)
    private String source = "MANUAL";

    @Column(name = "completed")
    private boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public WorkoutEvent() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getWorkoutType() { return workoutType; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getTargetPaceSeconds() { return targetPaceSeconds; }
    public String getNotes() { return notes; }
    public String getSource() { return source; }
    public boolean isCompleted() { return completed; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUser(User user) { this.user = user; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTargetPaceSeconds(Integer targetPaceSeconds) { this.targetPaceSeconds = targetPaceSeconds; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setSource(String source) { this.source = source; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
