// ========== StravaWebhookDTO.java ==========
package com.runnit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StravaWebhookDTO {
    
    @JsonProperty("aspect_type")
    private String aspectType; // "create", "update", "delete"
    
    @JsonProperty("event_time")
    private Long eventTime;
    
    @JsonProperty("object_id")
    private Long objectId; // Activity ID
    
    @JsonProperty("object_type")
    private String objectType; // "activity" or "athlete"
    
    @JsonProperty("owner_id")
    private Long ownerId; // Strava athlete ID
    
    @JsonProperty("subscription_id")
    private Long subscriptionId;
    
    @JsonProperty("updates")
    private Updates updates;
    
    @Data
    public static class Updates {
        private String title;
        private String type;
        private Boolean authorized;
    }
    
    @Data
    public static class VerificationResponse {
        @JsonProperty("hub.challenge")
        private String hubChallenge;
    }
}