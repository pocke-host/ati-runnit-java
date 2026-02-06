package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "follows")
@Data
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
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class FollowId implements Serializable {
    private Long followerUserId;
    private Long followingUserId;
}