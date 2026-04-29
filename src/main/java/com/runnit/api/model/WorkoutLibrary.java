package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "workout_library")
public class WorkoutLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No FK constraint — PlanetScale doesn't enforce them
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "workout_type", length = 50)
    private String workoutType;

    @Column(name = "sport", length = 50)
    private String sport;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "target_pace_seconds")
    private Integer targetPaceSeconds;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WorkoutLibrary() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getWorkoutType() { return workoutType; }
    public String getSport() { return sport; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getTargetPaceSeconds() { return targetPaceSeconds; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }
    public void setSport(String sport) { this.sport = sport; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTargetPaceSeconds(Integer targetPaceSeconds) { this.targetPaceSeconds = targetPaceSeconds; }
    public void setNotes(String notes) { this.notes = notes; }
}
