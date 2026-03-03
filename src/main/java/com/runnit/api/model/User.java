package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

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
}
