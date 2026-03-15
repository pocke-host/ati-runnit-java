package com.runnit.api.dto;

import com.runnit.api.model.Activity;
import jakarta.validation.constraints.*;

public class ActivityRequest {

    @NotNull(message = "Sport type is required")
    private Activity.SportType sportType;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    @Min(value = 0, message = "Distance cannot be negative")
    private Integer distanceMeters;

    private Integer elevationGain;
    private Integer elevationMeters; // alias for elevationGain from frontend
    private Integer calories;
    private Integer averageHeartRate;
    private Double averagePace;
    private String routePolyline;
    private Double startLat;
    private Double startLng;

    public ActivityRequest() {}

    public Activity.SportType getSportType() { return sportType; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getElevationGain() { return elevationGain != null ? elevationGain : elevationMeters; }
    public Integer getElevationMeters() { return elevationMeters; }
    public Integer getCalories() { return calories; }
    public Integer getAverageHeartRate() { return averageHeartRate; }
    public Double getAveragePace() { return averagePace; }
    public String getRoutePolyline() { return routePolyline; }
    public Double getStartLat() { return startLat; }
    public Double getStartLng() { return startLng; }

    public void setSportType(Activity.SportType sportType) { this.sportType = sportType; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setElevationGain(Integer elevationGain) { this.elevationGain = elevationGain; }
    public void setElevationMeters(Integer elevationMeters) { this.elevationMeters = elevationMeters; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public void setAverageHeartRate(Integer averageHeartRate) { this.averageHeartRate = averageHeartRate; }
    public void setAveragePace(Double averagePace) { this.averagePace = averagePace; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }
    public void setStartLng(Double startLng) { this.startLng = startLng; }
}
