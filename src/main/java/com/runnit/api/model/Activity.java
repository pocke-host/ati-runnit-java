package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private Source source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SportType {
        RUN, BIKE, SWIM, HIKE, WALK, OTHER
    }

    public enum Source {
        MANUAL, GARMIN, STRAVA, APPLE_WATCH
    }
}