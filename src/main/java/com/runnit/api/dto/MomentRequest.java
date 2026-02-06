// ========== MomentRequest.java ==========
package com.runnit.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MomentRequest {
    
    private Long activityId;
    
    @NotBlank(message = "Photo URL is required")
    private String photoUrl;
    
    private String routeSnapshotUrl;
    
    @NotBlank(message = "Song title is required")
    @Size(max = 255, message = "Song title too long")
    private String songTitle;
    
    @NotBlank(message = "Song artist is required")
    @Size(max = 255, message = "Song artist too long")
    private String songArtist;
    
    @Size(max = 500, message = "Song link too long")
    private String songLink;
}