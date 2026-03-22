package com.runnit.api.dto;

import java.time.Instant;

public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String location;
    private String sport;
    private String primarySport;
    private String bio;
    private Boolean isPublic;
    private String role;
    private Boolean onboardingComplete;
    private Long followerCount;
    private Long followingCount;
    private Long activityCount;
    private Instant createdAt;
    private String unitSystem;
    private String archetype;
    private String subscriptionStatus;
    private String subscriptionTier;

    public UserResponse() {}

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLocation() { return location; }
    public String getSport() { return sport; }
    public String getPrimarySport() { return primarySport; }
    public String getBio() { return bio; }
    public Boolean getIsPublic() { return isPublic; }
    public String getRole() { return role; }
    public Boolean getOnboardingComplete() { return onboardingComplete; }
    public Long getFollowerCount() { return followerCount; }
    public Long getFollowingCount() { return followingCount; }
    public Long getActivityCount() { return activityCount; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUnitSystem() { return unitSystem; }
    public String getArchetype() { return archetype; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public String getSubscriptionTier() { return subscriptionTier; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setLocation(String location) { this.location = location; }
    public void setSport(String sport) { this.sport = sport; }
    public void setPrimarySport(String primarySport) { this.primarySport = primarySport; }
    public void setBio(String bio) { this.bio = bio; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public void setRole(String role) { this.role = role; }
    public void setOnboardingComplete(Boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
    public void setActivityCount(Long activityCount) { this.activityCount = activityCount; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }
    public void setArchetype(String archetype) { this.archetype = archetype; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String email;
        private String displayName;
        private String avatarUrl;
        private String location;
        private String sport;
        private String primarySport;
        private String bio;
        private Boolean isPublic;
        private String role;
        private Boolean onboardingComplete;
        private Long followerCount;
        private Long followingCount;
        private Long activityCount;
        private Instant createdAt;
        private String unitSystem;
        private String archetype;
        private String subscriptionStatus;
        private String subscriptionTier;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder sport(String sport) { this.sport = sport; return this; }
        public Builder primarySport(String primarySport) { this.primarySport = primarySport; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public Builder isPublic(Boolean isPublic) { this.isPublic = isPublic; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder onboardingComplete(Boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; return this; }
        public Builder followerCount(Long followerCount) { this.followerCount = followerCount; return this; }
        public Builder followingCount(Long followingCount) { this.followingCount = followingCount; return this; }
        public Builder activityCount(Long activityCount) { this.activityCount = activityCount; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder unitSystem(String unitSystem) { this.unitSystem = unitSystem; return this; }
        public Builder archetype(String archetype) { this.archetype = archetype; return this; }
        public Builder subscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; return this; }
        public Builder subscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }

        public UserResponse build() {
            UserResponse r = new UserResponse();
            r.id = this.id;
            r.email = this.email;
            r.displayName = this.displayName;
            r.avatarUrl = this.avatarUrl;
            r.location = this.location;
            r.sport = this.sport;
            r.primarySport = this.primarySport != null ? this.primarySport : this.sport;
            r.bio = this.bio;
            r.isPublic = this.isPublic;
            r.role = this.role;
            r.onboardingComplete = this.onboardingComplete;
            r.followerCount = this.followerCount;
            r.followingCount = this.followingCount;
            r.activityCount = this.activityCount;
            r.createdAt = this.createdAt;
            r.unitSystem = this.unitSystem;
            r.archetype = this.archetype;
            r.subscriptionStatus = this.subscriptionStatus;
            r.subscriptionTier = this.subscriptionTier;
            return r;
        }
    }
}
