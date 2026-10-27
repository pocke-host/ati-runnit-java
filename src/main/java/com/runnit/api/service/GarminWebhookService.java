package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.model.WellnessDaily;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ingests Garmin push payloads. Garmin can deliver activities, daily health,
 * sleep, and other enabled datasets in one callback. Records are resolved by
 * Garmin user id when available, with the access-token fallback retained for
 * older payloads and existing connections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GarminWebhookService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final WellnessDailyRepository wellnessDailyRepository;
    private final AdaptivePlanService adaptivePlanService;
    private final AutoMomentService autoMomentService;

    @Transactional
    public int processPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return 0;
        int saved = 0;
        Set<Long> touchedUsers = new HashSet<>();

        for (Map<String, Object> activity : records(payload, "activities")) {
            Optional<User> user = resolveUser(activity);
            if (user.isEmpty()) continue;
            try {
                if (saveActivity(user.get(), activity)) saved++;
                touchedUsers.add(user.get().getId());
            } catch (Exception e) {
                log.warn("Garmin activity webhook record failed: {}", e.getMessage());
            }
        }

        for (Map<String, Object> daily : records(payload, "dailies")) {
            Optional<User> user = resolveUser(daily);
            if (user.isEmpty()) continue;
            try {
                saveDaily(user.get(), daily);
                touchedUsers.add(user.get().getId());
            } catch (Exception e) {
                log.warn("Garmin daily webhook record failed: {}", e.getMessage());
            }
        }

        for (Map<String, Object> sleep : records(payload, "sleeps")) {
            Optional<User> user = resolveUser(sleep);
            if (user.isEmpty()) continue;
            try {
                saveSleep(user.get(), sleep);
                touchedUsers.add(user.get().getId());
            } catch (Exception e) {
                log.warn("Garmin sleep webhook record failed: {}", e.getMessage());
            }
        }

        // Women’s Health and other enabled datasets may not have a matching
        // Runnit table yet, but they still prove the connection is alive.
        for (String key : List.of("womenHealth", "women_health", "mct")) {
            for (Map<String, Object> record : records(payload, key)) {
                resolveUser(record).map(User::getId).ifPresent(touchedUsers::add);
            }
        }

        if (!touchedUsers.isEmpty()) {
            Instant now = Instant.now();
            for (Long userId : touchedUsers) {
                userRepository.findById(userId).ifPresent(user -> {
                    user.setGarminLastSync(now);
                    userRepository.save(user);
                });
            }
        }
        log.info("Garmin webhook processed activity records: {}", saved);
        return saved;
    }

    /** Backward-compatible entry point for callers/tests that submit activities only. */
    @Transactional
    public int processActivities(List<Map<String, Object>> activities) {
        return processPayload(Map.of("activities", activities));
    }

    private Optional<User> resolveUser(Map<String, Object> record) {
        String garminUserId = firstString(record, "userId", "user_id", "garminUserId");
        if (garminUserId != null) {
            Optional<User> byId = userRepository.findByGarminUserId(garminUserId);
            if (byId.isPresent()) return byId;
        }
        String accessToken = firstString(record, "userAccessToken", "user_access_token", "accessToken");
        if (accessToken != null) return userRepository.findByGarminAccessToken(accessToken);
        log.warn("Garmin webhook record did not contain a recognized user identifier");
        return Optional.empty();
    }

    private boolean saveActivity(User user, Map<String, Object> record) {
        String activityId = firstString(record, "activityId", "activity_id");
        if (activityId == null) return false;
        String externalId = "garmin_" + activityId;
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        String rawType = firstString(record, "activityType", "activity_type", "sportType", "sport");
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setExternalId(externalId);
        activity.setSource(Activity.Source.GARMIN);
        activity.setSportType(mapSportType(rawType));
        // Garmin includes duration for normal Activity API summaries. Keep a
        // zero-length summary if a partial delivery omits it so the activity
        // is still visible and can be enriched by a later delivery.
        activity.setDurationSeconds(defaultInt(firstInt(record, "durationInSeconds", "duration"), 0));
        activity.setDistanceMeters(firstMeters(record, "distanceInMeters", "distance"));
        activity.setElevationGain(firstMeters(record, "elevationGainInMeters", "elevationGain"));
        activity.setCalories(firstInt(record, "calories", "activeKilocalories"));
        activity.setAverageHeartRate(firstInt(record, "averageHeartRateInBeatsPerMinute", "averageHR"));
        activity.setMaxHeartRate(firstInt(record, "maxHeartRateInBeatsPerMinute", "maxHR"));
        activity.setAveragePace(firstDouble(record, "averageSpeedInMetersPerSecond", "averageSpeed"));
        activity.setAverageCadence(firstDouble(record,
                "averageBikeCadenceInRoundsPerMinute",
                "averageRunCadenceInStepsPerMinute",
                "averageCadence"));
        activity.setPerformedAt(parseStart(record));
        String activityName = firstString(record, "activityName", "activity_name", "name");
        activity.setNotes(rawType != null
                ? "GARMIN: " + titleCase(rawType) + (activityName == null ? "" : " — " + activityName)
                : activityName == null ? null : "GARMIN: " + activityName);
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

    private void saveDaily(User user, Map<String, Object> record) {
        LocalDate date = parseDate(record);
        WellnessDaily row = wellnessDailyRepository.findByUserIdAndDateAndSource(user.getId(), date, "GARMIN")
                .orElseGet(WellnessDaily::new);
        row.setUserId(user.getId());
        row.setDate(date);
        row.setSource("GARMIN");
        row.setExternalCycleId(firstString(record, "summaryId", "dailySummaryId", "calendarDate"));
        row.setRestingHeartRate(firstInt(record, "restingHeartRateInBeatsPerMinute", "restingHeartRate"));
        row.setHrvMilli(firstDouble(record, "hrvWeeklyAverage", "hrvValue", "heartRateVariability"));
        // Garmin stress is stored separately; do not present its 0–100 scale as
        // WHOOP-style strain on the dashboard.
        row.setStrain(firstDouble(record, "bodyBatteryImpact", "trainingLoad"));
        row.setSteps(firstInt(record, "steps", "totalSteps"));
        Integer intensity = firstInt(record, "intensityMinutes", "intensityMinutesTotal");
        if (intensity == null) {
            intensity = sum(firstInt(record, "moderateIntensityMinutes"),
                    firstInt(record, "vigorousIntensityMinutes"));
        }
        row.setIntensityMinutes(intensity);
        row.setActiveKilocalories(firstInt(record, "activeKilocalories", "activeKCalories"));
        row.setAverageStress(firstDouble(record, "averageStressLevel", "stressLevel"));
        row.setBodyBattery(firstInt(record, "bodyBatteryMostRecentValue", "bodyBatteryValue", "bodyBattery"));
        row.setRespirationBreathsPerMinute(firstDouble(record, "averageRespirationValue", "averageRespiration"));
        row.setPulseOxPercent(firstDouble(record, "averageSpo2", "averageSpO2", "pulseOx"));
        wellnessDailyRepository.save(row);
    }

    private void saveSleep(User user, Map<String, Object> record) {
        LocalDate date = parseDate(record);
        WellnessDaily row = wellnessDailyRepository.findByUserIdAndDateAndSource(user.getId(), date, "GARMIN")
                .orElseGet(WellnessDaily::new);
        row.setUserId(user.getId());
        row.setDate(date);
        row.setSource("GARMIN");
        row.setExternalCycleId(firstString(record, "sleepSummaryId", "summaryId", "calendarDate"));
        Integer total = secondsToMinutes(firstDouble(record, "sleepingDurationInSeconds", "durationInSeconds"));
        Integer light = secondsToMinutes(firstDouble(record, "lightSleepDurationInSeconds"));
        Integer deep = secondsToMinutes(firstDouble(record, "deepSleepDurationInSeconds"));
        Integer rem = secondsToMinutes(firstDouble(record, "remSleepDurationInSeconds"));
        Integer awake = secondsToMinutes(firstDouble(record, "awakeDurationInSeconds"));
        if (total != null) row.setTotalSleepMinutes(total);
        if (light != null) row.setLightSleepMinutes(light);
        if (deep != null) row.setDeepSleepMinutes(deep);
        if (rem != null) row.setRemSleepMinutes(rem);
        if (awake != null) row.setAwakeMinutes(awake);
        row.setSleepEfficiencyPct(firstDouble(record, "sleepEfficiencyPercentage", "sleepEfficiency"));
        wellnessDailyRepository.save(row);
    }

    private LocalDateTime parseStart(Map<String, Object> record) {
        Number seconds = firstNumber(record, "startTimeInSeconds", "start_time_in_seconds");
        if (seconds != null) {
            Number offset = firstNumber(record, "startTimeOffsetInSeconds", "start_time_offset_in_seconds");
            int offsetSeconds = offset == null ? 0 : offset.intValue();
            return LocalDateTime.ofEpochSecond(seconds.longValue(), 0, ZoneOffset.ofTotalSeconds(offsetSeconds));
        }
        String start = firstString(record, "startTimeGMT", "startTimeLocal");
        if (start != null) {
            try { return LocalDateTime.parse(start.replace('T', ' ').replace("Z", "")); }
            catch (Exception ignored) { }
        }
        return LocalDateTime.now();
    }

    private LocalDate parseDate(Map<String, Object> record) {
        String date = firstString(record, "calendarDate", "date");
        if (date != null) {
            try { return LocalDate.parse(date.substring(0, Math.min(10, date.length()))); }
            catch (Exception ignored) { }
        }
        return parseStart(record).toLocalDate();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> records(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        List<Map<String, Object>> records;
        if (value instanceof Collection<?> collection) {
            records = collection.stream().filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .map(record -> withPayloadIdentity(payload, record))
                    .toList();
        } else if (value instanceof Map<?, ?> single) {
            records = List.of(withPayloadIdentity(payload, (Map<String, Object>) single));
        } else {
            records = List.of();
        }
        return records;
    }

    /**
     * Garmin normally repeats the user identifier on every record. Some
     * delivery shapes put it at the envelope level instead, so carry that
     * context down before resolving the Runnit user. This keeps webhook
     * delivery resilient without storing or logging the raw payload.
     */
    private Map<String, Object> withPayloadIdentity(Map<String, Object> payload, Map<String, Object> record) {
        if (firstString(record, "userId", "user_id", "garminUserId", "userAccessToken", "user_access_token", "accessToken") != null) {
            return record;
        }
        Map<String, Object> enriched = new java.util.LinkedHashMap<>(record);
        copyIfPresent(payload, enriched, "userId", "user_id", "garminUserId");
        copyIfPresent(payload, enriched, "userAccessToken", "user_access_token", "accessToken");
        return enriched;
    }

    private void copyIfPresent(Map<String, Object> from, Map<String, Object> to, String... keys) {
        for (String key : keys) {
            if (from.containsKey(key) && from.get(key) != null) {
                to.putIfAbsent(key, from.get(key));
                return;
            }
        }
    }

    private String firstString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?> nested) {
                for (String nestedKey : List.of("typeKey", "value", "id", "userId", "userAccessToken")) {
                    Object nestedValue = nested.get(nestedKey);
                    if (nestedValue != null && !nestedValue.toString().isBlank()) return nestedValue.toString();
                }
            }
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return null;
    }

    private Number firstNumber(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) return number;
            if (value != null) try { return Double.parseDouble(value.toString()); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private Integer firstInt(Map<String, Object> map, String... keys) {
        Number value = firstNumber(map, keys);
        return value == null ? null : value.intValue();
    }

    private Integer defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private Integer firstMeters(Map<String, Object> map, String... keys) {
        Number value = firstNumber(map, keys);
        return value == null ? null : (int) Math.round(value.doubleValue());
    }

    private Double firstDouble(Map<String, Object> map, String... keys) {
        Number value = firstNumber(map, keys);
        return value == null ? null : value.doubleValue();
    }

    private Integer secondsToMinutes(Double seconds) {
        return seconds == null ? null : (int) Math.round(seconds / 60d);
    }

    private Integer sum(Integer first, Integer second) {
        if (first == null && second == null) return null;
        return (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    private String titleCase(String value) {
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private Activity.SportType mapSportType(String type) {
        if (type == null) return Activity.SportType.OTHER;
        String value = type.toLowerCase();
        if (value.contains("run") || value.contains("trail")) return Activity.SportType.RUN;
        if (value.contains("cycl") || value.contains("bike") || value.contains("ride")) return Activity.SportType.BIKE;
        if (value.contains("swim")) return Activity.SportType.SWIM;
        if (value.contains("hike")) return Activity.SportType.HIKE;
        if (value.contains("walk")) return Activity.SportType.WALK;
        if (value.contains("strength")) return Activity.SportType.STRENGTH;
        return Activity.SportType.OTHER;
    }
}
