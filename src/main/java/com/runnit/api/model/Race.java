package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "races")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 5K, 10K, HALF_MARATHON, MARATHON, ULTRA, TRIATHLON, IRONMAN, OTHER
    @Column(name = "race_type", nullable = false)
    private String raceType;

    @Column(name = "sport_type", nullable = false)
    private String sportType = "RUN";

    @Column(name = "race_date", nullable = false)
    private LocalDate raceDate;

    @Column(name = "location_name")
    private String locationName;

    private String city;
    private String state;
    private String country = "US";

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "registration_url", length = 1000)
    private String registrationUrl;

    @Column(name = "organizer_name")
    private String organizerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_user_id")
    private User organizerUser;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "price_cents")
    private Integer priceCents;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
