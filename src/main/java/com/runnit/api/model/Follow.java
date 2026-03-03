package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "follows")
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public Long getFollowerUserId() { return followerUserId; }
    public Long getFollowingUserId() { return followingUserId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFollowerUserId(Long followerUserId) { this.followerUserId = followerUserId; }
    public void setFollowingUserId(Long followingUserId) { this.followingUserId = followingUserId; }
}
