package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false)
    private SportType sportType;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "elevation_gain")
    private Integer elevationGain;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "average_pace")
    private Double averagePace;

    @Column(name = "route_polyline", columnDefinition = "TEXT")
    private String routePolyline;

    @Column(name = "start_lat")
    private Double startLat;

    @Column(name = "start_lng")
    private Double startLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private Source source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Activity() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public SportType getSportType() { return sportType; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getCalories() { return calories; }
    public Integer getElevationGain() { return elevationGain; }
    public Integer getAverageHeartRate() { return averageHeartRate; }
    public Integer getMaxHeartRate() { return maxHeartRate; }
    public Double getAveragePace() { return averagePace; }
    public String getRoutePolyline() { return routePolyline; }
    public Double getStartLat() { return startLat; }
    public Double getStartLng() { return startLng; }
    public Source getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setSportType(SportType sportType) { this.sportType = sportType; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public void setElevationGain(Integer elevationGain) { this.elevationGain = elevationGain; }
    public void setAverageHeartRate(Integer averageHeartRate) { this.averageHeartRate = averageHeartRate; }
    public void setMaxHeartRate(Integer maxHeartRate) { this.maxHeartRate = maxHeartRate; }
    public void setAveragePace(Double averagePace) { this.averagePace = averagePace; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }
    public void setStartLng(Double startLng) { this.startLng = startLng; }
    public void setSource(Source source) { this.source = source; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public void setNotes(String notes) { this.notes = notes; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private SportType sportType;
        private Integer durationSeconds;
        private Integer distanceMeters;
        private Integer calories;
        private Integer elevationGain;
        private Integer averageHeartRate;
        private Integer maxHeartRate;
        private Double averagePace;
        private String routePolyline;
        private Double startLat;
        private Double startLng;
        private Source source;
        private String externalId;
        private String notes;

        public Builder user(User user) { this.user = user; return this; }
        public Builder sportType(SportType sportType) { this.sportType = sportType; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder distanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; return this; }
        public Builder calories(Integer calories) { this.calories = calories; return this; }
        public Builder elevationGain(Integer elevationGain) { this.elevationGain = elevationGain; return this; }
        public Builder averageHeartRate(Integer averageHeartRate) { this.averageHeartRate = averageHeartRate; return this; }
        public Builder maxHeartRate(Integer maxHeartRate) { this.maxHeartRate = maxHeartRate; return this; }
        public Builder averagePace(Double averagePace) { this.averagePace = averagePace; return this; }
        public Builder routePolyline(String routePolyline) { this.routePolyline = routePolyline; return this; }
        public Builder startLat(Double startLat) { this.startLat = startLat; return this; }
        public Builder startLng(Double startLng) { this.startLng = startLng; return this; }
        public Builder source(Source source) { this.source = source; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }

        public Activity build() {
            Activity a = new Activity();
            a.user = this.user;
            a.sportType = this.sportType;
            a.durationSeconds = this.durationSeconds;
            a.distanceMeters = this.distanceMeters;
            a.calories = this.calories;
            a.elevationGain = this.elevationGain;
            a.averageHeartRate = this.averageHeartRate;
            a.maxHeartRate = this.maxHeartRate;
            a.averagePace = this.averagePace;
            a.routePolyline = this.routePolyline;
            a.startLat = this.startLat;
            a.startLng = this.startLng;
            a.source = this.source;
            a.externalId = this.externalId;
            a.notes = this.notes;
            return a;
        }
    }

    public enum SportType { RUN, BIKE, SWIM, HIKE, WALK, OTHER }
    public enum Source { MANUAL, GARMIN, STRAVA, APPLE_WATCH }
}
