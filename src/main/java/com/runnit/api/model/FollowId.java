package com.runnit.api.model;

import java.io.Serializable;
import java.util.Objects;

public class FollowId implements Serializable {
    private Long followerUserId;
    private Long followingUserId;

    public FollowId() {}
    public FollowId(Long followerUserId, Long followingUserId) {
        this.followerUserId = followerUserId;
        this.followingUserId = followingUserId;
    }

    public Long getFollowerUserId() { return followerUserId; }
    public Long getFollowingUserId() { return followingUserId; }
    public void setFollowerUserId(Long followerUserId) { this.followerUserId = followerUserId; }
    public void setFollowingUserId(Long followingUserId) { this.followingUserId = followingUserId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId)) return false;
        FollowId that = (FollowId) o;
        return Objects.equals(followerUserId, that.followerUserId) && Objects.equals(followingUserId, that.followingUserId);
    }

    @Override
    public int hashCode() { return Objects.hash(followerUserId, followingUserId); }
}
