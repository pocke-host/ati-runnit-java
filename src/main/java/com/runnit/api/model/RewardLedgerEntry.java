package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "rewards_ledger", uniqueConstraints = @UniqueConstraint(name = "uq_rewards_ledger_user_event", columnNames = {"user_id", "event_key"}))
public class RewardLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "points_delta", nullable = false) private Integer pointsDelta;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "event_key", nullable = false) private String eventKey;
    @Column(nullable = false) private String description;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public Long getId(){return id;} public User getUser(){return user;} public Integer getPointsDelta(){return pointsDelta;}
    public String getEventType(){return eventType;} public String getEventKey(){return eventKey;} public String getDescription(){return description;}
    public Instant getCreatedAt(){return createdAt;}
    public static RewardLedgerEntry of(User user, int points, String type, String key, String description){
        RewardLedgerEntry e=new RewardLedgerEntry(); e.user=user; e.pointsDelta=points; e.eventType=type; e.eventKey=key; e.description=description; return e;
    }
}
