// ========== StravaActivityDetailDTO.java ==========
package com.runnit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StravaActivityDetailDTO {
    
    private Long id;
    private String name;
    private String type;
    
    @JsonProperty("distance")
    private Double distance; // meters
    
    @JsonProperty("moving_time")
    private Integer movingTime; // seconds
    
    @JsonProperty("elapsed_time")
    private Integer elapsedTime;
    
    @JsonProperty("total_elevation_gain")
    private Double totalElevationGain; // meters
    
    @JsonProperty("start_date")
    private LocalDateTime startDate;
    
    @JsonProperty("average_speed")
    private Double averageSpeed; // m/s
    
    @JsonProperty("max_speed")
    private Double maxSpeed;
    
    @JsonProperty("average_heartrate")
    private Double averageHeartrate;
    
    @JsonProperty("max_heartrate")
    private Double maxHeartrate;
    
    @JsonProperty("calories")
    private Double calories;
    
    @JsonProperty("map")
    private MapData map;
    
    @Data
    public static class MapData {
        private String id;
        private String polyline;
        @JsonProperty("summary_polyline")
        private String summaryPolyline;
    }
}