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
    private final AdaptivePlanService adaptivePlanService;
    private final AutoMomentService autoMomentService;

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

        String rawType = getString(act, "activityType");
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setExternalId(externalId);
        activity.setSource(Activity.Source.GARMIN);
        activity.setSportType(mapSportType(rawType));
        activity.setDurationSeconds(getInt(act, "durationInSeconds"));
        activity.setDistanceMeters(getMeters(act, "distanceInMeters"));
        activity.setElevationGain(getMeters(act, "elevationGainInMeters"));
        activity.setCalories(getInt(act, "calories"));
        activity.setAverageHeartRate(getInt(act, "averageHeartRateInBeatsPerMinute"));
        activity.setMaxHeartRate(getInt(act, "maxHeartRateInBeatsPerMinute"));
        activity.setAveragePace(getDouble(act, "averageSpeedInMetersPerSecond"));
        activity.setPerformedAt(parseGarminStart(act));
        // sport_type is a fixed DB enum — preserve the raw label so nothing's silently lost when
        // it falls to OTHER, matching the same convention WhoopService uses for its ~100 sport names.
        activity.setNotes(rawType != null ? "GARMIN: " + titleCase(rawType) : null);
        activityRepository.save(activity);
        try {
            adaptivePlanService.onActivityRecorded(activity);
        } catch (Exception e) {
            log.warn("Adaptive plan evaluation failed for Garmin activity {}: {}", externalId, e.getMessage());
        }
        try {
            autoMomentService.onActivityRecorded(activity);
        } catch (Exception e) {
            log.warn("Auto-moment creation failed for Garmin activity {}: {}", externalId, e.getMessage());
        }
        return true;
    }

    /**
     * startTimeInSeconds/startTimeOffsetInSeconds are confirmed against Garmin's Health API
     * "Activity Summary" webhook spec — correct for this payload. Some activity types
     * (e.g. manually-logged wellness entries with no captured start time) omit the field though,
     * so this still needs a real fallback instead of leaving performedAt null — a null previously
     * surfaced as "Invalid Date" in the UI instead of "just now".
     */
    private LocalDateTime parseGarminStart(Map<String, Object> act) {
        Object startRaw = act.get("startTimeInSeconds");
        if (startRaw instanceof Number n) {
            long offsetSeconds = 0;
            Object offsetRaw = act.get("startTimeOffsetInSeconds");
            if (offsetRaw instanceof Number o) offsetSeconds = o.longValue();
            return LocalDateTime.ofEpochSecond(n.longValue(), 0, ZoneOffset.ofTotalSeconds((int) offsetSeconds));
        }
        return LocalDateTime.now();
    }

    private String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private Activity.SportType mapSportType(String type) {
        if (type == null) return Activity.SportType.OTHER;
        String t = type.toLowerCase();
        if (t.contains("run") || t.contains("trail"))  return Activity.SportType.RUN;
        if (t.contains("cycl") || t.contains("bike") || t.contains("ride")) return Activity.SportType.BIKE;
        if (t.contains("swim"))                        return Activity.SportType.SWIM;
        if (t.contains("hike"))                        return Activity.SportType.HIKE;
        if (t.contains("walk"))                        return Activity.SportType.WALK;
        if (t.contains("strength"))                    return Activity.SportType.STRENGTH;
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
