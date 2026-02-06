package com.runnit.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Activity basics
    @Column(nullable = false)
    private String type; // e.g., RUN, RIDE, SWIM

    @Column(nullable = false)
    private double distance; // meters or kilometers

    @Column(nullable = false)
    private double duration; // seconds or minutes

    private double elevationGain;
    private double averagePace; // min/km or similar
    private double calories;

    // Start and end timestamps
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Relation back to user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Optional metadata
    private String title;
    private String description;
    private String location;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Activity() {}

    public Activity(User user, String type, double distance, double duration, LocalDateTime startTime) {
        this.user = user;
        this.type = type;
        this.distance = distance;
        this.duration = duration;
        this.startTime = startTime;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public double getElevationGain() { return elevationGain; }
    public void setElevationGain(double elevationGain) { this.elevationGain = elevationGain; }

    public double getAveragePace() { return averagePace; }
    public void setAveragePace(double averagePace) { this.averagePace = averagePace; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
