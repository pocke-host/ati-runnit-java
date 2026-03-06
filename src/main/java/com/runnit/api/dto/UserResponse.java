package com.runnit.api.dto;

import java.time.Instant;

public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String location;
    private String sport;
    private Long followerCount;
    private Long followingCount;
    private Long activityCount;
    private Instant createdAt;
    private String unitSystem;

    public UserResponse() {}

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLocation() { return location; }
    public String getSport() { return sport; }
    public Long getFollowerCount() { return followerCount; }
    public Long getFollowingCount() { return followingCount; }
    public Long getActivityCount() { return activityCount; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUnitSystem() { return unitSystem; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setLocation(String location) { this.location = location; }
    public void setSport(String sport) { this.sport = sport; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
    public void setActivityCount(Long activityCount) { this.activityCount = activityCount; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String email;
        private String displayName;
        private String avatarUrl;
        private String location;
        private String sport;
        private Long followerCount;
        private Long followingCount;
        private Long activityCount;
        private Instant createdAt;
        private String unitSystem;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder sport(String sport) { this.sport = sport; return this; }
        public Builder followerCount(Long followerCount) { this.followerCount = followerCount; return this; }
        public Builder followingCount(Long followingCount) { this.followingCount = followingCount; return this; }
        public Builder activityCount(Long activityCount) { this.activityCount = activityCount; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder unitSystem(String unitSystem) { this.unitSystem = unitSystem; return this; }

        public UserResponse build() {
            UserResponse r = new UserResponse();
            r.id = this.id;
            r.email = this.email;
            r.displayName = this.displayName;
            r.avatarUrl = this.avatarUrl;
            r.location = this.location;
            r.sport = this.sport;
            r.followerCount = this.followerCount;
            r.followingCount = this.followingCount;
            r.activityCount = this.activityCount;
            r.createdAt = this.createdAt;
            r.unitSystem = this.unitSystem;
            return r;
        }
    }
}
