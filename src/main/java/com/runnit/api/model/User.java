package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(name = "user", nullable = false)
    private String user;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "location")
    private String location;

    @Column(name = "sport")
    private String sport;

    @Column(name = "unit_system")
    private String unitSystem = "metric";

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "role")
    private String role = "athlete";

    @Column(name = "onboarding_complete")
    private Boolean onboardingComplete = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public User() {}

    public enum AuthProvider {
        GOOGLE, APPLE, EMAIL
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUser() { return user; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getProviderId() { return providerId; }
    public String getPasswordHash() { return passwordHash; }
    public String getLocation() { return location; }
    public String getSport() { return sport; }
    public String getUnitSystem() { return unitSystem; }
    public String getBio() { return bio; }
    public Boolean getIsPublic() { return isPublic; }
    public String getRole() { return role; }
    public Boolean getOnboardingComplete() { return onboardingComplete; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setUser(String user) { this.user = user; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setLocation(String location) { this.location = location; }
    public void setSport(String sport) { this.sport = sport; }
    public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }
    public void setBio(String bio) { this.bio = bio; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public void setRole(String role) { this.role = role; }
    public void setOnboardingComplete(Boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String email;
        private String user;
        private String displayName;
        private String avatarUrl;
        private AuthProvider authProvider;
        private String providerId;
        private String passwordHash;
        private String location;
        private String sport;
        private String unitSystem = "metric";

        public Builder email(String email) { this.email = email; return this; }
        public Builder user(String user) { this.user = user; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder authProvider(AuthProvider authProvider) { this.authProvider = authProvider; return this; }
        public Builder providerId(String providerId) { this.providerId = providerId; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder sport(String sport) { this.sport = sport; return this; }
        public Builder unitSystem(String unitSystem) { this.unitSystem = unitSystem; return this; }

        public User build() {
            User u = new User();
            u.email = this.email;
            u.user = this.user;
            u.displayName = this.displayName;
            u.avatarUrl = this.avatarUrl;
            u.authProvider = this.authProvider;
            u.providerId = this.providerId;
            u.passwordHash = this.passwordHash;
            u.location = this.location;
            u.sport = this.sport;
            u.unitSystem = this.unitSystem;
            return u;
        }
    }
}
