package com.runnit.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public class UserResponse {
    private Long id;
    private String email;
    private String user;        // username handle (e.g. "quinnhuynh")
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
    private Boolean lifetimePro;
    private Boolean isFollowing;
    private String onboardingGoal;
    private String experienceLevel;
    private String trainingDays;
    private String targetEventId;
    private String targetEventProvider;
    private String targetEventName;
    private LocalDate targetEventDate;
    private String targetEventSport;
    private String targetEventUrl;
    private String targetEventCity;
    private String targetEventState;

    public UserResponse() {}

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUser() { return user; }
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
    public Boolean getLifetimePro() { return lifetimePro; }
    public Boolean getIsFollowing() { return isFollowing; }
    public String getOnboardingGoal() { return onboardingGoal; }
    public String getExperienceLevel() { return experienceLevel; }
    public String getTrainingDays() { return trainingDays; }
    public String getTargetEventId() { return targetEventId; }
    public String getTargetEventProvider() { return targetEventProvider; }
    public String getTargetEventName() { return targetEventName; }
    public LocalDate getTargetEventDate() { return targetEventDate; }
    public String getTargetEventSport() { return targetEventSport; }
    public String getTargetEventUrl() { return targetEventUrl; }
    public String getTargetEventCity() { return targetEventCity; }
    public String getTargetEventState() { return targetEventState; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setUser(String user) { this.user = user; }
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
    public void setLifetimePro(Boolean lifetimePro) { this.lifetimePro = lifetimePro; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
    public void setOnboardingGoal(String value) { this.onboardingGoal = value; }
    public void setExperienceLevel(String value) { this.experienceLevel = value; }
    public void setTrainingDays(String value) { this.trainingDays = value; }
    public void setTargetEventId(String value) { this.targetEventId = value; }
    public void setTargetEventProvider(String value) { this.targetEventProvider = value; }
    public void setTargetEventName(String value) { this.targetEventName = value; }
    public void setTargetEventDate(LocalDate value) { this.targetEventDate = value; }
    public void setTargetEventSport(String value) { this.targetEventSport = value; }
    public void setTargetEventUrl(String value) { this.targetEventUrl = value; }
    public void setTargetEventCity(String value) { this.targetEventCity = value; }
    public void setTargetEventState(String value) { this.targetEventState = value; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String email;
        private String user;
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
        private Boolean lifetimePro;
        private Boolean isFollowing;
        private String onboardingGoal;
        private String experienceLevel;
        private String trainingDays;
        private String targetEventId;
        private String targetEventProvider;
        private String targetEventName;
        private LocalDate targetEventDate;
        private String targetEventSport;
        private String targetEventUrl;
        private String targetEventCity;
        private String targetEventState;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder user(String user) { this.user = user; return this; }
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
        public Builder lifetimePro(Boolean lifetimePro) { this.lifetimePro = lifetimePro; return this; }
        public Builder isFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; return this; }
        public Builder onboardingGoal(String value) { this.onboardingGoal = value; return this; }
        public Builder experienceLevel(String value) { this.experienceLevel = value; return this; }
        public Builder trainingDays(String value) { this.trainingDays = value; return this; }
        public Builder targetEventId(String value) { this.targetEventId = value; return this; }
        public Builder targetEventProvider(String value) { this.targetEventProvider = value; return this; }
        public Builder targetEventName(String value) { this.targetEventName = value; return this; }
        public Builder targetEventDate(LocalDate value) { this.targetEventDate = value; return this; }
        public Builder targetEventSport(String value) { this.targetEventSport = value; return this; }
        public Builder targetEventUrl(String value) { this.targetEventUrl = value; return this; }
        public Builder targetEventCity(String value) { this.targetEventCity = value; return this; }
        public Builder targetEventState(String value) { this.targetEventState = value; return this; }

        public UserResponse build() {
            UserResponse r = new UserResponse();
            r.id = this.id;
            r.email = this.email;
            r.user = this.user;
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
            r.lifetimePro = this.lifetimePro;
            r.isFollowing = this.isFollowing;
            r.onboardingGoal = this.onboardingGoal;
            r.experienceLevel = this.experienceLevel;
            r.trainingDays = this.trainingDays;
            r.targetEventId = this.targetEventId;
            r.targetEventProvider = this.targetEventProvider;
            r.targetEventName = this.targetEventName;
            r.targetEventDate = this.targetEventDate;
            r.targetEventSport = this.targetEventSport;
            r.targetEventUrl = this.targetEventUrl;
            r.targetEventCity = this.targetEventCity;
            r.targetEventState = this.targetEventState;
            return r;
        }
    }
}
