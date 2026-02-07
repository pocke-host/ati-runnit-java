// ========== GarminWebhookService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.GarminActivityDTO;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
// import com.runnit.api.model.Activity.enums.SportType; 
// Assuming SportType is an enum inside Activity
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.runnit.api.service.AutoMomentService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarminWebhookService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final MomentService momentService;
    private final AutoMomentService autoMomentService;

    @Transactional
    public void processGarminActivity(GarminActivityDTO dto) {
        log.info("Processing Garmin activity: {}", dto.getActivityId());
        
        // Find user by Garmin access token (you'll need to store this during Garmin OAuth)
        User user = userRepository.findByGarminAccessToken(dto.getUserAccessToken())
                .orElseThrow(() -> new RuntimeException("User not found for Garmin token"));
        
        // Create Activity
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setSportType(mapGarminSportType(dto.getActivityType()));
        activity.setDurationSeconds(dto.getDurationInSeconds());
        activity.setDistanceMeters(dto.getDistanceInMeters() != null ? dto.getDistanceInMeters().intValue() : null);
        activity.setCalories(dto.getCalories());
        activity.setElevationGain(dto.getElevationGainInMeters() != null ? dto.getElevationGainInMeters().intValue() : null);
        activity.setAverageHeartRate(dto.getAverageHeartRate());
        activity.setMaxHeartRate(dto.getMaxHeartRate());
        activity.setAveragePace(calculatePace(dto.getAverageSpeed()));
        activity.setRoutePolyline(dto.getGeoPolyline());
        
        if (dto.getStartTimeInSeconds() != null) {
            activity.setCreatedAt(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(dto.getStartTimeInSeconds()), 
                ZoneId.systemDefault()
            ));
        }
        
        Activity savedActivity = activityRepository.save(activity);
        log.info("Created activity {} from Garmin webhook", savedActivity.getId());
        autoMomentService.autoGenerateMomentFromActivity(savedActivity);
        
        // TODO: Auto-generate moment if user has this setting enabled
        // momentService.autoGenerateMomentFromActivity(savedActivity);
    }
    
    private SportType mapGarminSportType(String garminType) {
        if (garminType == null) return SportType.OTHER;
        
        return switch (garminType.toLowerCase()) {
            case "running", "trail_running", "treadmill_running" -> SportType.RUN;
            case "cycling", "road_cycling", "mountain_biking" -> SportType.BIKE;
            case "swimming", "lap_swimming", "open_water_swimming" -> SportType.SWIM;
            case "hiking" -> SportType.HIKE;
            case "walking" -> SportType.WALK;
            default -> SportType.OTHER;
        };
    }
    
    private Double calculatePace(Double speedMetersPerSecond) {
        if (speedMetersPerSecond == null || speedMetersPerSecond == 0) return null;
        // Pace in minutes per km
        return (1000.0 / speedMetersPerSecond) / 60.0;
    }
}