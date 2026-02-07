// ========== StravaTokenResponseDTO.java ==========
package com.runnit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StravaTokenResponseDTO {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("expires_at")
    private Long expiresAt;
    
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("athlete")
    private Athlete athlete;
    
    @Data
    public static class Athlete {
        private Long id;
        private String username;
        
        @JsonProperty("firstname")
        private String firstName;
        
        @JsonProperty("lastname")
        private String lastName;
        
        private String city;
        private String state;
        private String country;
    }
}