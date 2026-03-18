package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "coach_messages")
public class CoachMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No FK constraints — PlanetScale doesn't support them
    @Column(name = "coach_id", nullable = false)
    private Long coachId;

    @Column(name = "athlete_id", nullable = false)
    private Long athleteId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_read")
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CoachMessage() {}

    public Long getId() { return id; }
    public Long getCoachId() { return coachId; }
    public Long getAthleteId() { return athleteId; }
    public Long getSenderId() { return senderId; }
    public String getBody() { return body; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public void setBody(String body) { this.body = body; }
    public void setRead(boolean read) { isRead = read; }
}
