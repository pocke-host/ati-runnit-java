package com.runnit.api.service;

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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Processes incoming Garmin Health API webhook events.
 * Garmin pushes activity summaries here immediately after a device syncs,
 * keyed by userAccessToken so we can map back to a Runnit user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GarminWebhookService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @Transactional
    public int processActivities(List<Map<String, Object>> activities) {
        int saved = 0;
        for (Map<String, Object> act : activities) {
            try {
                String token = (String) act.get("userAccessToken");
                if (token == null) continue;

                var userOpt = userRepository.findByGarminAccessToken(token);
                if (userOpt.isEmpty()) {
                    log.warn("Garmin webhook: no user found for access token");
                    continue;
                }

                if (saveActivity(userOpt.get(), act)) saved++;
            } catch (Exception e) {
                log.warn("Garmin webhook: failed to process activity — {}", e.getMessage());
            }
        }
        log.info("Garmin webhook: saved {}/{} activities", saved, activities.size());
        return saved;
    }

    private boolean saveActivity(User user, Map<String, Object> act) {
        String externalId = "garmin_" + act.get("activityId");
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        Activity activity = new Activity();
        activity.setUser(user);
        activity.setExternalId(externalId);
        activity.setSource(Activity.Source.GARMIN);
        activity.setSportType(mapSportType(getString(act, "activityType")));
        activity.setDurationSeconds(getInt(act, "durationInSeconds"));
        activity.setDistanceMeters(getMeters(act, "distanceInMeters"));
        activity.setElevationGain(getMeters(act, "elevationGainInMeters"));
        activity.setCalories(getInt(act, "calories"));
        activity.setAverageHeartRate(getInt(act, "averageHeartRateInBeatsPerMinute"));
        activity.setMaxHeartRate(getInt(act, "maxHeartRateInBeatsPerMinute"));
        activity.setAveragePace(getDouble(act, "averageSpeedInMetersPerSecond"));
        activity.setPerformedAt(parseGarminStart(act));
        activityRepository.save(activity);
        return true;
    }

    /**
     * startTimeInSeconds/startTimeOffsetInSeconds follow Garmin Health API's documented naming
     * convention (matches durationInSeconds/distanceInMeters already used in this payload) — not
     * verified against a live payload capture, so if the field name is actually different this
     * just falls back to null (activity still saves, performedAt defaults to sync time as before).
     */
    private LocalDateTime parseGarminStart(Map<String, Object> act) {
        Object startRaw = act.get("startTimeInSeconds");
        if (!(startRaw instanceof Number)) return null;
        long offsetSeconds = 0;
        Object offsetRaw = act.get("startTimeOffsetInSeconds");
        if (offsetRaw instanceof Number n) offsetSeconds = n.longValue();
        return LocalDateTime.ofEpochSecond(((Number) startRaw).longValue(), 0, ZoneOffset.ofTotalSeconds((int) offsetSeconds));
    }

    private Activity.SportType mapSportType(String type) {
        if (type == null) return Activity.SportType.OTHER;
        String t = type.toLowerCase();
        if (t.contains("run") || t.contains("trail"))  return Activity.SportType.RUN;
        if (t.contains("cycl") || t.contains("bike") || t.contains("ride")) return Activity.SportType.BIKE;
        if (t.contains("swim"))                        return Activity.SportType.SWIM;
        if (t.contains("hike"))                        return Activity.SportType.HIKE;
        if (t.contains("walk"))                        return Activity.SportType.WALK;
        return Activity.SportType.OTHER;
    }

    private String getString(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> m, String k) {
        Object v = m.get(k); return v instanceof Number n ? n.intValue() : null;
    }

    private Integer getMeters(Map<String, Object> m, String k) {
        Object v = m.get(k); return v instanceof Number n ? (int) Math.round(n.doubleValue()) : null;
    }

    private Double getDouble(Map<String, Object> m, String k) {
        Object v = m.get(k); return v instanceof Number n ? n.doubleValue() : null;
    }
}
