package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "live_location_shares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveLocationShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "last_latitude", precision = 10, scale = 8)
    private BigDecimal lastLatitude;

    @Column(name = "last_longitude", precision = 11, scale = 8)
    private BigDecimal lastLongitude;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "share_token", nullable = false, unique = true)
    private String shareToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
