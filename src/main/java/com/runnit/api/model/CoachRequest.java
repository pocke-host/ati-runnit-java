package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "coach_requests")
public class CoachRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No FK constraints — PlanetScale doesn't support them
    @Column(name = "coach_id", nullable = false)
    private Long coachId;

    @Column(name = "athlete_id", nullable = false)
    private Long athleteId;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CoachRequest() {}

    public Long getId() { return id; }
    public Long getCoachId() { return coachId; }
    public Long getAthleteId() { return athleteId; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public void setStatus(String status) { this.status = status; }
}
