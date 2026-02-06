package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "moments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(name = "route_snapshot_url")
    private String routeSnapshotUrl;

    @Column(name = "song_title", nullable = false)
    private String songTitle;

    @Column(name = "song_artist", nullable = false)
    private String songArtist;

    @Column(name = "song_link")
    private String songLink;

    @Column(name = "day_key", nullable = false)
    private LocalDate dayKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}