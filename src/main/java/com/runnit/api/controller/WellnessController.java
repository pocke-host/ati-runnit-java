package com.runnit.api.controller;

import com.runnit.api.model.WellnessDaily;
import com.runnit.api.model.Activity;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessDailyRepository wellnessDailyRepository;
    private final ActivityRepository activityRepository;

    /** GET /api/wellness/today — most recent day's recovery/sleep/strain snapshot */
    @GetMapping("/today")
    public ResponseEntity<?> getToday(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            LocalDate today = LocalDate.now();
            // Fall back a few days in case today's cycle hasn't scored yet (e.g. still asleep)
            for (int i = 0; i < 3; i++) {
                LocalDate date = today.minusDays(i);
                List<WellnessDaily> rows = wellnessDailyRepository.findByUserIdAndDate(userId, date);
                if (!rows.isEmpty()) {
                    return ResponseEntity.ok(toMap(preferredRow(rows)));
                }
            }
            return ResponseEntity.ok(Map.of("available", false));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/wellness?start=YYYY-MM-DD&end=YYYY-MM-DD — history for charting */
    @GetMapping
    public ResponseEntity<?> getRange(
            @RequestParam String start,
            @RequestParam String end,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<Map<String, Object>> days = wellnessDailyRepository
                    .findByUserIdAndDateBetweenOrderByDateDesc(userId, LocalDate.parse(start), LocalDate.parse(end))
                    .stream().map(this::toMap).toList();
            return ResponseEntity.ok(days);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Compare same-day WHOOP and Garmin signals without collapsing their
     * provider-specific semantics into one score.
     *
     * GET /api/wellness/compare?days=30
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compare(
            @RequestParam(defaultValue = "30") int days,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            int boundedDays = Math.max(7, Math.min(days, 90));
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(boundedDays - 1L);
            List<WellnessDaily> wellnessRows = wellnessDailyRepository
                    .findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end);
            LocalDateTime startAt = start.atStartOfDay();
            LocalDateTime endAt = end.plusDays(1).atStartOfDay();
            List<Activity> activities = activityRepository.findByUserIdBetween(userId, startAt, endAt);

            Map<String, List<WellnessDaily>> bySource = wellnessRows.stream()
                    .filter(row -> "WHOOP".equals(row.getSource()) || "GARMIN".equals(row.getSource()))
                    .collect(Collectors.groupingBy(WellnessDaily::getSource));
            Map<String, List<Activity>> activityBySource = activities.stream()
                    .filter(activity -> activity.getSource() == Activity.Source.WHOOP || activity.getSource() == Activity.Source.GARMIN)
                    .collect(Collectors.groupingBy(activity -> activity.getSource().name()));

            Map<LocalDate, WellnessDaily> whoopByDate = indexByDate(bySource.getOrDefault("WHOOP", List.of()));
            Map<LocalDate, WellnessDaily> garminByDate = indexByDate(bySource.getOrDefault("GARMIN", List.of()));
            Set<LocalDate> matchedDates = new HashSet<>(whoopByDate.keySet());
            matchedDates.retainAll(garminByDate.keySet());

            List<Map<String, Object>> metrics = new ArrayList<>();
            addMetric(metrics, "restingHeartRate", "Resting heart rate", "bpm", matchedDates,
                    whoopByDate, garminByDate, WellnessDaily::getRestingHeartRate);
            addMetric(metrics, "hrvMilli", "Heart rate variability", "ms", matchedDates,
                    whoopByDate, garminByDate, WellnessDaily::getHrvMilli);
            addMetric(metrics, "totalSleepMinutes", "Sleep duration", "min", matchedDates,
                    whoopByDate, garminByDate, WellnessDaily::getTotalSleepMinutes);
            addMetric(metrics, "sleepEfficiencyPct", "Sleep efficiency", "%", matchedDates,
                    whoopByDate, garminByDate, WellnessDaily::getSleepEfficiencyPct);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("available", activityBySource.containsKey("WHOOP") && activityBySource.containsKey("GARMIN")
                    || bySource.containsKey("WHOOP") && bySource.containsKey("GARMIN"));
            response.put("rangeStart", start.toString());
            response.put("rangeEnd", end.toString());
            response.put("daysRequested", boundedDays);
            response.put("daysWithBothWellness", matchedDates.size());
            response.put("sources", List.of("WHOOP", "GARMIN"));
            response.put("sourceSummary", Map.of(
                    "WHOOP", sourceSummary("WHOOP", bySource.getOrDefault("WHOOP", List.of()), activityBySource.getOrDefault("WHOOP", List.of())),
                    "GARMIN", sourceSummary("GARMIN", bySource.getOrDefault("GARMIN", List.of()), activityBySource.getOrDefault("GARMIN", List.of()))
            ));
            response.put("activityMetrics", activityMetrics(
                    sourceSummary("WHOOP", bySource.getOrDefault("WHOOP", List.of()), activityBySource.getOrDefault("WHOOP", List.of())),
                    sourceSummary("GARMIN", bySource.getOrDefault("GARMIN", List.of()), activityBySource.getOrDefault("GARMIN", List.of()))
            ));
            response.put("metrics", metrics);
            response.put("contextSignals", contextSignals(whoopByDate, garminByDate, matchedDates));
            response.put("notes", List.of(
                    "Recovery and Body Battery are provider-specific signals and are shown side-by-side without a calculated delta.",
                    "Only days with data from both providers are used for same-day metric differences."
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private WellnessDaily preferredRow(List<WellnessDaily> rows) {
        return rows.stream()
                .sorted(Comparator.comparingInt(row -> switch (row.getSource()) {
                    case "WHOOP" -> 0;
                    case "OURA" -> 1;
                    case "GARMIN" -> 2;
                    case "FITBIT" -> 3;
                    default -> 4;
                }))
                .findFirst()
                .orElse(rows.get(0));
    }

    private Map<LocalDate, WellnessDaily> indexByDate(List<WellnessDaily> rows) {
        return rows.stream().collect(Collectors.toMap(WellnessDaily::getDate, row -> row, (first, ignored) -> first));
    }

    private void addMetric(
            List<Map<String, Object>> metrics,
            String key,
            String label,
            String unit,
            Set<LocalDate> matchedDates,
            Map<LocalDate, WellnessDaily> whoopByDate,
            Map<LocalDate, WellnessDaily> garminByDate,
            java.util.function.Function<WellnessDaily, Number> value) {
        List<Double> whoop = matchedDates.stream().map(whoopByDate::get).map(value)
                .filter(java.util.Objects::nonNull).map(Number::doubleValue)
                .filter(number -> !Double.isNaN(number) && number > 0).toList();
        List<Double> garmin = matchedDates.stream().map(garminByDate::get).map(value)
                .filter(java.util.Objects::nonNull).map(Number::doubleValue)
                .filter(number -> !Double.isNaN(number) && number > 0).toList();
        Double whoopAverage = average(whoop);
        Double garminAverage = average(garmin);
        if (whoopAverage == null && garminAverage == null) return;
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("key", key);
        metric.put("label", label);
        metric.put("unit", unit);
        metric.put("whoop", whoopAverage);
        metric.put("garmin", garminAverage);
        metric.put("difference", whoopAverage != null && garminAverage != null ? round(garminAverage - whoopAverage) : null);
        metric.put("whoopDays", whoop.size());
        metric.put("garminDays", garmin.size());
        metrics.add(metric);
    }

    private List<Map<String, Object>> contextSignals(
            Map<LocalDate, WellnessDaily> whoopByDate,
            Map<LocalDate, WellnessDaily> garminByDate,
            Set<LocalDate> matchedDates) {
        List<Double> whoopRecovery = matchedDates.stream().map(whoopByDate::get).map(WellnessDaily::getRecoveryScore)
                .filter(java.util.Objects::nonNull).map(Integer::doubleValue).toList();
        List<Double> garminBattery = matchedDates.stream().map(garminByDate::get).map(WellnessDaily::getBodyBattery)
                .filter(java.util.Objects::nonNull).map(Integer::doubleValue).toList();
        if (whoopRecovery.isEmpty() && garminBattery.isEmpty()) return List.of();
        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("key", "recoveryContext");
        signal.put("label", "Recovery / Body Battery");
        signal.put("whoop", average(whoopRecovery));
        signal.put("garmin", average(garminBattery));
        signal.put("unit", "/100");
        signal.put("comparable", false);
        signal.put("note", "Different provider scales — use these as context, not a winner.");
        return List.of(signal);
    }

    private Map<String, Object> sourceSummary(String source, List<WellnessDaily> wellness, List<Activity> activities) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("source", source);
        summary.put("wellnessDays", wellness.size());
        summary.put("activityCount", activities.size());
        summary.put("durationSeconds", activities.stream().map(Activity::getDurationSeconds).filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum());
        summary.put("distanceMeters", activities.stream().map(Activity::getDistanceMeters).filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum());
        summary.put("averageHeartRate", averageInt(activities, Activity::getAverageHeartRate));
        summary.put("maxHeartRate", maxInt(activities, Activity::getMaxHeartRate));
        summary.put("averageSleepMinutes", average(wellness.stream().map(WellnessDaily::getTotalSleepMinutes).filter(java.util.Objects::nonNull).map(Integer::doubleValue).toList()));
        summary.put("lastActivityAt", activities.stream().map(Activity::getPerformedAt).filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).map(LocalDateTime::toString).orElse(null));
        summary.put("lastWellnessDate", wellness.stream().map(WellnessDaily::getDate).max(Comparator.naturalOrder()).map(LocalDate::toString).orElse(null));
        return summary;
    }

    private List<Map<String, Object>> activityMetrics(Map<String, Object> whoop, Map<String, Object> garmin) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        addActivityMetric(metrics, "trainingTime", "Training time", "h", whoop, garmin, "durationSeconds", 3600d);
        addActivityMetric(metrics, "distance", "Distance", "km", whoop, garmin, "distanceMeters", 1000d);
        addActivityMetric(metrics, "averageHeartRate", "Average heart rate", "bpm", whoop, garmin, "averageHeartRate", 1d);
        return metrics;
    }

    private void addActivityMetric(
            List<Map<String, Object>> metrics,
            String key,
            String label,
            String unit,
            Map<String, Object> whoop,
            Map<String, Object> garmin,
            String field,
            double divisor) {
        Double whoopValue = number(whoop.get(field), divisor);
        Double garminValue = number(garmin.get(field), divisor);
        if (whoopValue == null && garminValue == null) return;
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("key", key);
        metric.put("label", label);
        metric.put("unit", unit);
        metric.put("whoop", whoopValue == null ? null : round(whoopValue));
        metric.put("garmin", garminValue == null ? null : round(garminValue));
        metric.put("difference", whoopValue != null && garminValue != null ? round(garminValue - whoopValue) : null);
        metrics.add(metric);
    }

    private Double number(Object value, double divisor) {
        return value instanceof Number number ? number.doubleValue() / divisor : null;
    }

    private Double averageInt(List<Activity> activities, java.util.function.Function<Activity, Integer> getter) {
        return average(activities.stream().map(getter).filter(java.util.Objects::nonNull).map(Integer::doubleValue).toList());
    }

    private Integer maxInt(List<Activity> activities, java.util.function.Function<Activity, Integer> getter) {
        return activities.stream().map(getter).filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(null);
    }

    private Double average(List<Double> values) {
        return values.isEmpty() ? null : round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private double round(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private Map<String, Object> toMap(WellnessDaily w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", w.getDate().toString());
        m.put("source", w.getSource());
        m.put("recoveryScore", w.getRecoveryScore());
        m.put("hrvMilli", w.getHrvMilli());
        m.put("restingHeartRate", w.getRestingHeartRate());
        m.put("sleepPerformancePct", w.getSleepPerformancePct());
        m.put("sleepEfficiencyPct", w.getSleepEfficiencyPct());
        m.put("totalSleepMinutes", w.getTotalSleepMinutes());
        m.put("lightSleepMinutes", w.getLightSleepMinutes());
        m.put("deepSleepMinutes", w.getDeepSleepMinutes());
        m.put("remSleepMinutes", w.getRemSleepMinutes());
        m.put("awakeMinutes", w.getAwakeMinutes());
        m.put("strain", w.getStrain());
        m.put("steps", w.getSteps());
        m.put("intensityMinutes", w.getIntensityMinutes());
        m.put("activeKilocalories", w.getActiveKilocalories());
        m.put("averageStress", w.getAverageStress());
        m.put("bodyBattery", w.getBodyBattery());
        m.put("respirationBreathsPerMinute", w.getRespirationBreathsPerMinute());
        m.put("pulseOxPercent", w.getPulseOxPercent());
        return m;
    }
}
