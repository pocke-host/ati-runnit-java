package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "challenge_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
