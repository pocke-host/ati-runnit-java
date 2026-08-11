package com.runnit.api.service;

import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleHealthService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final AdaptivePlanService adaptivePlanService;
    private final AutoMomentService autoMomentService;

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).map(u -> {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", Boolean.TRUE.equals(u.getAppleHealthConnected()));
            status.put("lastSync", u.getAppleHealthLastSync() != null ? u.getAppleHealthLastSync().toString() : null);
            return status;
        }).orElse(Map.of("connected", false, "lastSync", null));
    }

    // HealthKit permissions are granted natively on-device — this just records that the
    // app completed that flow, mirroring the "connected" flag the OAuth integrations set
    // once their token exchange succeeds.
    @Transactional
    public void connect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setAppleHealthConnected(true);
        userRepository.save(user);
    }

    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setAppleHealthConnected(false);
        user.setAppleHealthLastSync(null);
        userRepository.save(user);
    }

    @Transactional
    public int syncActivities(Long userId, List<Map<String, Object>> samples) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int saved = 0;
        for (Map<String, Object> sample : samples) {
            try {
                if (saveActivity(user, sample)) saved++;
            } catch (Exception e) {
                log.warn("Apple Health sync: failed to process sample — {}", e.getMessage());
            }
        }

        user.setAppleHealthConnected(true);
        user.setAppleHealthLastSync(Instant.now());
        userRepository.save(user);

        log.info("Apple Health sync: saved {}/{} samples for user {}", saved, samples.size(), userId);
        return saved;
    }

    private boolean saveActivity(User user, Map<String, Object> sample) {
        Object rawExternalId = sample.get("externalId");
        if (rawExternalId == null) return false;

        String externalId = "apple_health_" + rawExternalId;
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        Activity activity = Activity.builder()
                .user(user)
                .source(Activity.Source.APPLE_HEALTH)
                .externalId(externalId)
                .sportType(mapSportType(sample.get("sportType")))
                .durationSeconds(getInt(sample, "durationSeconds"))
                .distanceMeters(getInt(sample, "distanceMeters"))
                .calories(getInt(sample, "calories"))
                .performedAt(parsePerformedAt(sample.get("performedAt")))
                .notes("APPLE HEALTH")
                .build();

        activityRepository.save(activity);

        try {
            adaptivePlanService.onActivityRecorded(activity);
        } catch (Exception e) {
            log.warn("Adaptive plan evaluation failed for Apple Health activity {}: {}", externalId, e.getMessage());
        }
        try {
            autoMomentService.onActivityRecorded(activity);
        } catch (Exception e) {
            log.warn("Auto-moment creation failed for Apple Health activity {}: {}", externalId, e.getMessage());
        }
        return true;
    }

    // The iOS app maps HealthKit's HKWorkoutActivityType to this closed set before sending —
    // an unrecognized/future value falls back to OTHER rather than throwing.
    private Activity.SportType mapSportType(Object raw) {
        if (raw == null) return Activity.SportType.OTHER;
        try {
            return Activity.SportType.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Activity.SportType.OTHER;
        }
    }

    private LocalDateTime parsePerformedAt(Object raw) {
        if (raw == null) return null;
        return OffsetDateTime.parse(raw.toString()).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return (int) Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
