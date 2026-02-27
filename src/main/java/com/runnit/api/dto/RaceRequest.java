package com.runnit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RaceRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String raceType;
    private String sportType = "RUN";
    @NotNull
    private LocalDate raceDate;
    private String locationName;
    private String city;
    private String state;
    private String country = "US";
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer distanceMeters;
    private String registrationUrl;
    private String organizerName;
    private String coverImageUrl;
    private Integer priceCents;
}
