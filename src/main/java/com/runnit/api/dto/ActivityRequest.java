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

    public ActivityRequest() {}

    public Activity.SportType getSportType() { return sportType; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getDistanceMeters() { return distanceMeters; }

    public void setSportType(Activity.SportType sportType) { this.sportType = sportType; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
}
