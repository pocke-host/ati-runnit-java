package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "challenge_participants")
@IdClass(ChallengeParticipantId.class)
public class ChallengeParticipant {

    @Id
    @Column(name = "challenge_id")
    private Long challengeId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "value", nullable = false)
    private double value = 0;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    public ChallengeParticipant() {}

    public Long getChallengeId() { return challengeId; }
    public Long getUserId() { return userId; }
    public double getValue() { return value; }
    public Instant getJoinedAt() { return joinedAt; }

    public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setValue(double value) { this.value = value; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long challengeId;
        private Long userId;
        private double value = 0;

        public Builder challengeId(Long challengeId) { this.challengeId = challengeId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder value(double value) { this.value = value; return this; }

        public ChallengeParticipant build() {
            ChallengeParticipant p = new ChallengeParticipant();
            p.challengeId = this.challengeId;
            p.userId = this.userId;
            p.value = this.value;
            return p;
        }
    }
}
