package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "garmin_access_token")
    private String garminAccessToken;

    @Column(name = "garmin_user_id")
    private String garminUserId;

    @Column(name = "strava_access_token")
    private String stravaAccessToken;

    @Column(name = "strava_refresh_token")
    private String stravaRefreshToken;

    @Column(name = "strava_athlete_id")
    private Long stravaAthleteId;

    @Column(name = "strava_token_expires_at")
    private LocalDateTime stravaTokenExpiresAt;

    @Column(name = "garmin_token_secret")
    private String garminTokenSecret;

    @Column(name = "spotify_access_token")
    private String spotifyAccessToken;

    @Column(name = "spotify_refresh_token")
    private String spotifyRefreshToken;

    public enum AuthProvider {
        GOOGLE, APPLE, EMAIL
    }
}