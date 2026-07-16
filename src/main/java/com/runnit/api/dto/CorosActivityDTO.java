package com.runnit.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorosActivityDTO {

    @JsonProperty("result")
    private String result;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Data data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("sportDataList")
        private List<SportData> sportDataList;

        @JsonProperty("count")
        private Integer count;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SportData {
        @JsonProperty("labelId")
        private String labelId;

        @JsonProperty("mode")
        private Integer mode;          // sport type code

        @JsonProperty("name")
        private String name;

        @JsonProperty("startTime")
        private Long startTime;        // epoch seconds

        @JsonProperty("endTime")
        private Long endTime;

        @JsonProperty("totalTime")
        private Integer totalTime;     // seconds

        @JsonProperty("distance")
        private Integer distance;      // meters

        @JsonProperty("calorie")
        private Integer calorie;

        @JsonProperty("avgHr")
        private Integer avgHr;

        @JsonProperty("maxHr")
        private Integer maxHr;

        @JsonProperty("totalAscent")
        private Integer totalAscent;   // meters

        @JsonProperty("avgSpeed")
        private Double avgSpeed;       // m/s
    }
}
