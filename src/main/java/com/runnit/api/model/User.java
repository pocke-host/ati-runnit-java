package com.runnit.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity                    // 👈 tells JPA this maps to a DB table
@Table(name = "users")     // 👈 optional, sets the actual table name
public class User {
     // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic account info
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    // Profile customization
    private String profilePictureUrl;
    private String bio;

    // Auth / Security
    @Column(nullable = false)
    private boolean mfaEnabled = false;

    @Column(nullable = false)
    private boolean emailVerified = false;

    private String status = "ACTIVE"; // ACTIVE, DISABLED, PENDING, etc.

    // Timestamps
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLoginAt;
    private LocalDateTime updatedAt;

    // Relationships (one user can have many activities, tokens, etc.)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Activity> activities = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    // Constructors
    public User() {}

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<Activity> getActivities() { return activities; }
    public void setActivities(Set<Activity> activities) { this.activities = activities; }

    public Set<RefreshToken> getRefreshTokens() { return refreshTokens; }
    public void setRefreshTokens(Set<RefreshToken> refreshTokens) { this.refreshTokens = refreshTokens; }
}
