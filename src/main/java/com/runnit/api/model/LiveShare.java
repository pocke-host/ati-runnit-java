package com.runnit.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_shares")
public class LiveShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "sport_type")
    private String sportType = "RUN";

    private Double lat;
    private Double lng;

    @Column(name = "elapsed_seconds")
    private Integer elapsedSeconds = 0;

    @Column(name = "distance_meters")
    private Integer distanceMeters = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LiveShare() {}

    public Long getId() { return id; }
    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getSportType() { return sportType; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public Integer getElapsedSeconds() { return elapsedSeconds; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setToken(String token) { this.token = token; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setSportType(String sportType) { this.sportType = sportType; }
    public void setLat(Double lat) { this.lat = lat; }
    public void setLng(Double lng) { this.lng = lng; }
    public void setElapsedSeconds(Integer elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
