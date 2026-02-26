// // ========== SpotifyService.java ==========
// package com.runnit.api.service;

// import com.runnit.api.dto.SongDTO;
// import com.runnit.api.model.Activity;
// import com.runnit.api.model.User;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// import java.time.Instant;
// import java.util.Map;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class SpotifyService {

//     private final RestTemplate restTemplate;
    
//     private static final String SPOTIFY_API_URL = "https://api.spotify.com/v1";

//     /**
//      * Get a track that was played during the activity time window
//      */
//     public SongDTO getRecentTrackDuringActivity(User user, Activity activity) {
//         log.info("Fetching Spotify track for activity time: {}", activity.getCreatedAt());
        
//         try {
//             // Get recently played tracks
//             String url = SPOTIFY_API_URL + "/me/player/recently-played?limit=50";
            
//             HttpHeaders headers = new HttpHeaders();
//             headers.setBearerAuth(user.getSpotifyAccessToken());
//             HttpEntity<?> entity = new HttpEntity<>(headers);
            
//             ResponseEntity<Map> response = restTemplate.exchange(
//                 url,
//                 HttpMethod.GET,
//                 entity,
//                 Map.class
//             );
            
//             Map<String, Object> body = response.getBody();
//             if (body == null || !body.containsKey("items")) {
//                 return null;
//             }
            
//             // Find track played during activity time
//             // Instant activityStart = activity.getCreatedAt();
//             // Instant activityEnd = activityStart.plusSeconds(activity.getDurationSeconds());
            
//             // TODO: Parse Spotify response and find matching track
//             // For now, return null to use fallback
            
//             return null;
            
//         } catch (Exception e) {
//             log.warn("Failed to fetch Spotify track: {}", e.getMessage());
//             return null;
//         }
//     }
// }