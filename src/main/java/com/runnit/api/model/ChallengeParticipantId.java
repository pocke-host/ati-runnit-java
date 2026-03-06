package com.runnit.api.model;

import java.io.Serializable;
import java.util.Objects;

public class ChallengeParticipantId implements Serializable {
    private Long challengeId;
    private Long userId;

    public ChallengeParticipantId() {}
    public ChallengeParticipantId(Long challengeId, Long userId) {
        this.challengeId = challengeId;
        this.userId = userId;
    }

    public Long getChallengeId() { return challengeId; }
    public Long getUserId() { return userId; }
    public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChallengeParticipantId)) return false;
        ChallengeParticipantId that = (ChallengeParticipantId) o;
        return Objects.equals(challengeId, that.challengeId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(challengeId, userId); }
}
