package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class RaceResponse {
    private Long id;
    private String name;
    private String description;
    private String raceType;
    private String sportType;
    private LocalDate raceDate;
    private String locationName;
    private String city;
    private String state;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer distanceMeters;
    private String registrationUrl;
    private String organizerName;
    private Long organizerUserId;
    private String coverImageUrl;
    private Integer priceCents;
    private boolean isFeatured;
    private long interestedCount;
    private String currentUserStatus; // null, INTERESTED, REGISTERED, COMPLETED
    private Instant createdAt;
}
