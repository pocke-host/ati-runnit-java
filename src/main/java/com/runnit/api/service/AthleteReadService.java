package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.WellnessDaily;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts the user's existing activity, wellness, and training-load data into
 * plain-language, evidence-backed coaching signals. This service intentionally
 * refuses to make a claim when the personal history is too small or stale.
 */
@Service
@RequiredArgsConstructor
public class AthleteReadService {

    private static final int WELLNESS_WINDOW_DAYS = 28;
    private static final int ACTIVITY_WINDOW_DAYS = 42;
    private static final int MIN_BASELINE_POINTS = 3;

    private final ActivityRepository activityRepository;
    private final WellnessDailyRepository wellnessDailyRepository;
    private final TrainingLoadService trainingLoadService;

    public Map<String, Object> compute(Long userId, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        List<Activity> activities = activityRepository.findByUserIdBetween(
                userId,
                today.minusDays(ACTIVITY_WINDOW_DAYS).atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        List<WellnessDaily> wellnessRows = wellnessDailyRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, today.minusDays(WELLNESS_WINDOW_DAYS - 1L), today);
        List<WellnessDaily> dailyWellness = preferredRowsByDate(wellnessRows);

        Map<String, Object> trainingLoad = trainingLoadService.computeMetrics(userId, zone);
        List<Map<String, Object>> insights = new ArrayList<>();

        WellnessDaily latestWellness = dailyWellness.stream()
                .max(Comparator.comparing(WellnessDaily::getDate))
                .orElse(null);
        if (latestWellness != null) {
            addSleepInsight(insights, latestWellness, dailyWellness);
            addRecoveryInsight(insights, latestWellness, dailyWellness);
        }

        addPerformanceInsight(insights, activities);
        addLoadInsight(insights, trainingLoad);

        insights.sort(Comparator.comparingInt(this::priority));
        List<Map<String, Object>> topInsights = insights.stream().limit(3).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", Instant.now().toString());
        response.put("available", !topInsights.isEmpty());
        response.put("windowDays", WELLNESS_WINDOW_DAYS);
        response.put("primary", topInsights.isEmpty() ? neutralInsight(activities.size(), dailyWellness.size()) : topInsights.get(0));
        response.put("insights", topInsights);
        response.put("dataQuality", dataQuality(activities, dailyWellness, latestWellness));
        response.put("sources", sources(dailyWellness));
        return response;
    }

    private void addSleepInsight(List<Map<String, Object>> insights, WellnessDaily latest, List<WellnessDaily> rows) {
        if (latest.getTotalSleepMinutes() == null) return;
        List<Integer> baseline = rows.stream()
                .filter(row -> row.getDate().isBefore(latest.getDate()))
                .map(WellnessDaily::getTotalSleepMinutes)
                .filter(Objects::nonNull)
                .limit(14)
                .toList();
        if (baseline.size() < MIN_BASELINE_POINTS) return;

        double baselineMinutes = median(baseline);
        double deltaMinutes = latest.getTotalSleepMinutes() - baselineMinutes;
        List<Map<String, Object>> evidence = List.of(
                evidence("Last night", formatSleep(latest.getTotalSleepMinutes())),
                evidence("Your 14-day baseline", formatSleep((int) Math.round(baselineMinutes))),
                evidence("Difference", formatSignedMinutes(deltaMinutes))
        );

        if (deltaMinutes <= -45) {
            insights.add(insight(
                    "sleep_below_baseline", "sleep", "caution", 10,
                    "Sleep was below your baseline.",
                    "You slept " + formatDurationDifference(-deltaMinutes) + " less than usual. Keep intensity controlled and give yourself a longer recovery window tonight.",
                    "Prioritize sleep tonight.", evidence, "high", baseline.size()));
        } else if (deltaMinutes >= 45) {
            insights.add(insight(
                    "sleep_above_baseline", "sleep", "positive", 40,
                    "You banked extra sleep.",
                    "You slept " + formatDurationDifference(deltaMinutes) + " more than your recent baseline. That supports the work already on your plan.",
                    "Follow the plan, but stay responsive to how you feel.", evidence, "high", baseline.size()));
        }
    }

    private void addRecoveryInsight(List<Map<String, Object>> insights, WellnessDaily latest, List<WellnessDaily> rows) {
        ReadinessSignal current = readinessSignal(latest);
        if (current == null) return;

        List<Integer> baseline = rows.stream()
                .filter(row -> row.getDate().isBefore(latest.getDate()))
                .map(this::readinessSignal)
                .filter(Objects::nonNull)
                .filter(signal -> signal.kind().equals(current.kind()))
                .map(ReadinessSignal::score)
                .limit(14)
                .toList();
        if (baseline.size() < MIN_BASELINE_POINTS) return;

        double baselineScore = median(baseline);
        double delta = current.score() - baselineScore;
        List<Map<String, Object>> evidence = List.of(
                evidence(current.label(), current.score() + "/100"),
                evidence("Your baseline", Math.round(baselineScore) + "/100"),
                evidence("Difference", (delta > 0 ? "+" : "") + Math.round(delta) + " points")
        );

        if (delta <= -8) {
            insights.add(insight(
                    "recovery_below_baseline", "recovery", "caution", 15,
                    current.label() + " is below your baseline.",
                    "Your latest " + current.label().toLowerCase() + " signal is " + Math.round(Math.abs(delta)) + " points below normal. Pair today’s plan with an easier effort or more recovery.",
                    "Choose easy movement or recovery.", evidence, "medium", baseline.size()));
        } else if (delta >= 8) {
            insights.add(insight(
                    "recovery_supports_training", "recovery", "positive", 45,
                    "Your recovery signal is trending well.",
                    "Your latest " + current.label().toLowerCase() + " signal is " + Math.round(delta) + " points above your baseline.",
                    "A good day to execute the planned session.", evidence, "medium", baseline.size()));
        }
    }

    private void addPerformanceInsight(List<Map<String, Object>> insights, List<Activity> activities) {
        if (activities.isEmpty()) return;
        List<Activity> ordered = activities.stream()
                .filter(activity -> activity.getPerformedAt() != null || activity.getCreatedAt() != null)
                .sorted(Comparator.comparing(this::activityTime).reversed())
                .toList();
        if (ordered.isEmpty()) return;

        Activity latest = ordered.get(0);
        List<Activity> similar = ordered.stream()
                .skip(1)
                .filter(previous -> previous.getSportType() == latest.getSportType())
                .filter(previous -> similarVolume(latest, previous))
                .limit(8)
                .toList();
        if (similar.size() < MIN_BASELINE_POINTS) return;

        Double latestPace = paceSecondsPerMeter(latest);
        List<Double> baselinePaces = similar.stream()
                .map(this::paceSecondsPerMeter)
                .filter(Objects::nonNull)
                .toList();
        if (latestPace == null || baselinePaces.size() < MIN_BASELINE_POINTS) return;

        double baselinePace = median(baselinePaces);
        double change = (latestPace - baselinePace) / baselinePace;
        List<Map<String, Object>> evidence = List.of(
                evidence("Latest similar activity", formatPace(latestPace)),
                evidence("Previous " + similar.size() + " similar activities", formatPace(baselinePace)),
                evidence("Sport", latest.getSportType() == null ? "Activity" : latest.getSportType().name().toLowerCase())
        );

        if (change >= 0.04) {
            insights.add(insight(
                    "performance_slipping", "performance", "caution", 20,
                    "Similar efforts are getting harder.",
                    "Your latest " + displaySport(latest) + " was about " + Math.round(change * 100) + "% slower than your recent comparable efforts. Check recovery, sleep, heat, hills, and effort before pushing harder.",
                    "Review the activity context before changing the plan.", evidence, "medium", similar.size()));
        } else if (change <= -0.04) {
            insights.add(insight(
                    "performance_improving", "performance", "positive", 50,
                    "You are improving at a similar effort.",
                    "Your latest " + displaySport(latest) + " was about " + Math.round(Math.abs(change) * 100) + "% faster than your comparable baseline.",
                    "Keep the progression gradual.", evidence, "medium", similar.size()));
        }
    }

    private void addLoadInsight(List<Map<String, Object>> insights, Map<String, Object> trainingLoad) {
        String riskLabel = String.valueOf(trainingLoad.getOrDefault("riskLabel", "BUILDING_BASELINE"));
        if ("BUILDING_BASELINE".equals(riskLabel)) return;

        String acwr = String.valueOf(trainingLoad.getOrDefault("acwr", "—"));
        String tsb = String.valueOf(trainingLoad.getOrDefault("tsb", "—"));
        List<Map<String, Object>> evidence = List.of(
                evidence("Load status", riskLabel.replace('_', ' ').toLowerCase()),
                evidence("ACWR", acwr),
                evidence("Form", tsb)
        );
        if ("HIGH_RISK".equals(riskLabel)) {
            insights.add(insight(
                    "training_load_elevated", "training load", "caution", 5,
                    "Your recent load is rising quickly.",
                    "Your short-term training is high relative to your baseline. An easy session or recovery day can protect the work you have already built.",
                    "Reduce intensity today.", evidence, "high", 42));
        } else if ("DETRAINING".equals(riskLabel)) {
            insights.add(insight(
                    "training_load_low", "training load", "info", 35,
                    "Your recent load is below your baseline.",
                    "You may be backing off or rebuilding. Increase gradually rather than trying to make up missed training in one session.",
                    "Rebuild consistency first.", evidence, "high", 42));
        }
    }

    private Map<String, Object> neutralInsight(int activityCount, int wellnessDays) {
        return insight(
                "build_personal_baseline", "baseline", "info", 100,
                "RUNNIT is learning your baseline.",
                "Keep logging workouts and connecting recovery data. After a few comparable sessions, RUNNIT can explain changes instead of showing raw numbers alone.",
                "Record your next activity.",
                List.of(evidence("Activities in view", String.valueOf(activityCount)), evidence("Wellness days in view", String.valueOf(wellnessDays))),
                "building", Math.max(activityCount, wellnessDays));
    }

    private Map<String, Object> insight(String key, String category, String severity, int priority,
                                        String title, String message, String action,
                                        List<Map<String, Object>> evidence, String confidence, int sampleSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("category", category);
        result.put("severity", severity);
        result.put("priority", priority);
        result.put("title", title);
        result.put("message", message);
        result.put("action", action);
        result.put("evidence", evidence);
        result.put("confidence", confidence);
        result.put("sampleSize", sampleSize);
        return result;
    }

    private Map<String, Object> evidence(String label, String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", label);
        result.put("value", value);
        return result;
    }

    private Map<String, Object> dataQuality(List<Activity> activities, List<WellnessDaily> wellness, WellnessDaily latest) {
        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("activityCount", activities.size());
        quality.put("wellnessDays", wellness.size());
        quality.put("latestWellnessDate", latest == null ? null : latest.getDate().toString());
        quality.put("latestWellnessSource", latest == null ? null : latest.getSource());
        quality.put("hasPersonalBaseline", wellness.size() >= MIN_BASELINE_POINTS || activities.size() >= MIN_BASELINE_POINTS);
        return quality;
    }

    private Set<String> sources(List<WellnessDaily> wellness) {
        return wellness.stream()
                .map(WellnessDaily::getSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<WellnessDaily> preferredRowsByDate(List<WellnessDaily> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(WellnessDaily::getDate, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::preferredRow)
                .sorted(Comparator.comparing(WellnessDaily::getDate).reversed())
                .toList();
    }

    private WellnessDaily preferredRow(List<WellnessDaily> rows) {
        return rows.stream()
                .sorted(Comparator
                        .comparing(WellnessDaily::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingInt(row -> sourcePriority(row.getSource())))
                .findFirst()
                .orElse(rows.get(0));
    }

    private int sourcePriority(String source) {
        return switch (source) {
            case "WHOOP" -> 0;
            case "OURA" -> 1;
            case "GARMIN" -> 2;
            case "FITBIT" -> 3;
            case "APPLE_HEALTH" -> 4;
            default -> 5;
        };
    }

    private ReadinessSignal readinessSignal(WellnessDaily row) {
        if (row.getRecoveryScore() != null) return new ReadinessSignal("recovery", "Recovery", clamp(row.getRecoveryScore()));
        if (row.getBodyBattery() != null) return new ReadinessSignal("body_battery", "Body Battery", clamp(row.getBodyBattery()));
        if (row.getSleepPerformancePct() != null) return new ReadinessSignal("sleep_performance", "Sleep performance", clamp(row.getSleepPerformancePct()));
        if (row.getSleepEfficiencyPct() != null) {
            double efficiency = row.getSleepEfficiencyPct();
            return new ReadinessSignal("sleep_efficiency", "Sleep efficiency", clamp((int) Math.round(efficiency <= 1 ? efficiency * 100 : efficiency)));
        }
        return null;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private LocalDateTime activityTime(Activity activity) {
        return activity.getPerformedAt() != null ? activity.getPerformedAt() : activity.getCreatedAt();
    }

    private boolean similarVolume(Activity current, Activity previous) {
        if (current.getDistanceMeters() != null && previous.getDistanceMeters() != null && current.getDistanceMeters() > 0) {
            return Math.abs(previous.getDistanceMeters() - current.getDistanceMeters()) / (double) current.getDistanceMeters() <= 0.15;
        }
        if (current.getDurationSeconds() != null && previous.getDurationSeconds() != null && current.getDurationSeconds() > 0) {
            return Math.abs(previous.getDurationSeconds() - current.getDurationSeconds()) / (double) current.getDurationSeconds() <= 0.20;
        }
        return false;
    }

    private Double paceSecondsPerMeter(Activity activity) {
        if (activity.getDistanceMeters() == null || activity.getDurationSeconds() == null || activity.getDistanceMeters() <= 0) return null;
        return activity.getDurationSeconds() / (double) activity.getDistanceMeters();
    }

    private String displaySport(Activity activity) {
        return activity.getSportType() == null ? "effort" : activity.getSportType().name().toLowerCase();
    }

    private String formatPace(double secondsPerMeter) {
        double secondsPerKm = secondsPerMeter * 1000;
        int minutes = (int) secondsPerKm / 60;
        int seconds = (int) Math.round(secondsPerKm % 60);
        if (seconds == 60) { minutes++; seconds = 0; }
        return minutes + ":" + String.format("%02d", seconds) + "/km";
    }

    private String formatSleep(int minutes) {
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private String formatDurationDifference(double minutes) {
        int rounded = (int) Math.round(Math.abs(minutes));
        return rounded >= 60 ? formatSleep(rounded) : rounded + " minutes";
    }

    private String formatSignedMinutes(double minutes) {
        int rounded = (int) Math.round(minutes);
        return (rounded > 0 ? "+" : "") + rounded + " min";
    }

    private double median(List<? extends Number> values) {
        List<Double> sorted = values.stream().map(Number::doubleValue).sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2d : sorted.get(middle);
    }

    private int priority(Map<String, Object> insight) {
        Object priority = insight.get("priority");
        return priority instanceof Number number ? number.intValue() : 100;
    }

    private record ReadinessSignal(String kind, String label, int score) {}
}
