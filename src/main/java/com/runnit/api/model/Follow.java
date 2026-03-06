package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "follows")
@IdClass(FollowId.class)
public class Follow {

    @Id
    @Column(name = "follower_user_id")
    private Long followerUserId;

    @Id
    @Column(name = "following_user_id")
    private Long followingUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Follow() {}

    public Long getFollowerUserId() { return followerUserId; }
    public Long getFollowingUserId() { return followingUserId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFollowerUserId(Long followerUserId) { this.followerUserId = followerUserId; }
    public void setFollowingUserId(Long followingUserId) { this.followingUserId = followingUserId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long followerUserId;
        private Long followingUserId;

        public Builder followerUserId(Long followerUserId) { this.followerUserId = followerUserId; return this; }
        public Builder followingUserId(Long followingUserId) { this.followingUserId = followingUserId; return this; }

        public Follow build() {
            Follow f = new Follow();
            f.followerUserId = this.followerUserId;
            f.followingUserId = this.followingUserId;
            return f;
        }
    }
}
