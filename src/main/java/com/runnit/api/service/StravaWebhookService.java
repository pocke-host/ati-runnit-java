// ========== StravaWebhookService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.StravaActivityDetailDTO;
import com.runnit.api.dto.StravaWebhookDTO;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.model.Activity.SportType;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class StravaWebhookService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    
    private static final String STRAVA_API_URL = "https://www.strava.com/api/v3";

    @Transactional
    public void processStravaEvent(StravaWebhookDTO dto) {
        if (!"activity".equals(dto.getObjectType())) {
            log.info("Ignoring non-activity event: {}", dto.getObjectType());
            return;
        }
        
        if ("create".equals(dto.getAspectType())) {
            handleActivityCreate(dto);
        } else if ("update".equals(dto.getAspectType())) {
            handleActivityUpdate(dto);
        } else if ("delete".equals(dto.getAspectType())) {
            handleActivityDelete(dto);
        }
    }
    
    private void handleActivityCreate(StravaWebhookDTO dto) {
        log.info("Processing Strava activity create: {}", dto.getObjectId());
        
        // Find user by Strava athlete ID
        User user = userRepository.findByStravaAthleteId(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found for Strava athlete ID: " + dto.getOwnerId()));
        
        // Fetch detailed activity data from Strava API
        StravaActivityDetailDTO activityDetail = fetchStravaActivityDetails(dto.getObjectId(), user.getStravaAccessToken());
        
        // Create Activity
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setSportType(mapStravaSportType(activityDetail.getType()));
        activity.setDurationSeconds(activityDetail.getMovingTime());
        activity.setDistanceMeters(activityDetail.getDistance() != null ? activityDetail.getDistance().intValue() : null);
        activity.setCalories(activityDetail.getCalories() != null ? activityDetail.getCalories().intValue() : null);
        activity.setElevationGain(activityDetail.getTotalElevationGain() != null ? activityDetail.getTotalElevationGain().intValue() : null);
        activity.setAverageHeartRate(activityDetail.getAverageHeartrate() != null ? activityDetail.getAverageHeartrate().intValue() : null);
        activity.setMaxHeartRate(activityDetail.getMaxHeartrate() != null ? activityDetail.getMaxHeartrate().intValue() : null);
        activity.setAveragePace(calculatePace(activityDetail.getAverageSpeed()));
        activity.setRoutePolyline(activityDetail.getMap() != null ? activityDetail.getMap().getPolyline() : null);
        
        if (activityDetail.getStartDate() != null) {
            activity.setCreatedAt(activityDetail.getStartDate());
        }
        
        Activity savedActivity = activityRepository.save(activity);
        log.info("Created activity {} from Strava webhook", savedActivity.getId());

        autoMomentService.autoGenerateMomentFromActivity(savedActivity);
    }
    
    private void handleActivityUpdate(StravaWebhookDTO dto) {
        log.info("Strava activity update not yet implemented: {}", dto.getObjectId());
        // TODO: Update existing activity
    }
    
    private void handleActivityDelete(StravaWebhookDTO dto) {
        log.info("Strava activity delete not yet implemented: {}", dto.getObjectId());
        // TODO: Delete activity
    }
    
    private StravaActivityDetailDTO fetchStravaActivityDetails(Long activityId, String accessToken) {
        String url = STRAVA_API_URL + "/activities/" + activityId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<StravaActivityDetailDTO> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            StravaActivityDetailDTO.class
        );
        
        return response.getBody();
    }
    
    private SportType mapStravaSportType(String stravaType) {
        if (stravaType == null) return SportType.OTHER;
        
        return switch (stravaType.toLowerCase()) {
            case "run", "trailrun", "virtualrun" -> SportType.RUN;
            case "ride", "virtualride", "mountainbikeride" -> SportType.BIKE;
            case "swim" -> SportType.SWIM;
            case "hike" -> SportType.HIKE;
            case "walk" -> SportType.WALK;
            default -> SportType.OTHER;
        };
    }
    
    private Double calculatePace(Double speedMetersPerSecond) {
        if (speedMetersPerSecond == null || speedMetersPerSecond == 0) return null;
        return (1000.0 / speedMetersPerSecond) / 60.0;
    }
}