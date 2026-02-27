package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class LiveLocationDTO {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private Long activityId;
    private BigDecimal lastLatitude;
    private BigDecimal lastLongitude;
    private Instant lastUpdated;
    private boolean isActive;
    private String shareToken;
    private String shareUrl;
    private Instant createdAt;
}
