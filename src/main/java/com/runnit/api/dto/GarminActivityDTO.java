// ========== GarminActivityDTO.java ==========
package com.runnit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GarminActivityDTO {
    
    @JsonProperty("userAccessToken")
    private String userAccessToken; // Maps to our user
    
    @JsonProperty("activityId")
    private String activityId;
    
    @JsonProperty("activityName")
    private String activityName;
    
    @JsonProperty("activityType")
    private String activityType; // "running", "cycling", "swimming", etc.
    
    @JsonProperty("startTimeInSeconds")
    private Long startTimeInSeconds;
    
    @JsonProperty("durationInSeconds")
    private Integer durationInSeconds;
    
    @JsonProperty("distanceInMeters")
    private Double distanceInMeters;
    
    @JsonProperty("elevationGainInMeters")
    private Double elevationGainInMeters;
    
    @JsonProperty("calories")
    private Integer calories;
    
    @JsonProperty("averageHeartRateInBeatsPerMinute")
    private Integer averageHeartRate;
    
    @JsonProperty("maxHeartRateInBeatsPerMinute")
    private Integer maxHeartRate;
    
    @JsonProperty("averageSpeedInMetersPerSecond")
    private Double averageSpeed;
    
    @JsonProperty("geoPolyline")
    private String geoPolyline; // Encoded route data
    
    @JsonProperty("summaryItems")
    private List<SummaryItem> summaryItems;
    
    @Data
    public static class SummaryItem {
        private String summaryId;
        private String summaryType;
        private String summaryValue;
    }
}