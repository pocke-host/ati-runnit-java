package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "sos_events")
public class SosEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private Double lat;

    @Column
    private Double lng;

    @Column(name = "share_url", length = 255)
    private String shareUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public SosEvent() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public String getShareUrl() { return shareUrl; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setLat(Double lat) { this.lat = lat; }
    public void setLng(Double lng) { this.lng = lng; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
}
