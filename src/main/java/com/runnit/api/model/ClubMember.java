package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "club_members")
@IdClass(ClubMember.ClubMemberId.class)
public class ClubMember {

    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    public ClubMember() {}

    public Long getClubId() { return clubId; }
    public Long getUserId() { return userId; }
    public Instant getJoinedAt() { return joinedAt; }

    public void setClubId(Long clubId) { this.clubId = clubId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long clubId;
        private Long userId;

        public Builder clubId(Long clubId) { this.clubId = clubId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }

        public ClubMember build() {
            ClubMember m = new ClubMember();
            m.clubId = this.clubId;
            m.userId = this.userId;
            return m;
        }
    }

    public static class ClubMemberId implements Serializable {
        private Long clubId;
        private Long userId;

        public ClubMemberId() {}
        public ClubMemberId(Long clubId, Long userId) { this.clubId = clubId; this.userId = userId; }

        public Long getClubId() { return clubId; }
        public Long getUserId() { return userId; }
        public void setClubId(Long clubId) { this.clubId = clubId; }
        public void setUserId(Long userId) { this.userId = userId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClubMemberId)) return false;
            ClubMemberId that = (ClubMemberId) o;
            return java.util.Objects.equals(clubId, that.clubId) && java.util.Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(clubId, userId); }
    }
}
