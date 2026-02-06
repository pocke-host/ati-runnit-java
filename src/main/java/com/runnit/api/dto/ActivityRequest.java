// ========== ActivityRequest.java ==========
package com.runnit.api.dto;

import com.runnit.api.model.Activity;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ActivityRequest {
    
    @NotNull(message = "Sport type is required")
    private Activity.SportType sportType;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;
    
    @Min(value = 0, message = "Distance cannot be negative")
    private Integer distanceMeters;
}