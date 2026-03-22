package com.runnit.api.service;

import com.runnit.api.dto.PerformanceResponse;
import com.runnit.api.model.Activity;
import com.runnit.api.model.PersonalRecord;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.PersonalRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes the full performance intelligence summary for an athlete.
 *
 * All calculations are derived from existing activity and personal-record data —
 * no external APIs are required. Algorithms used:
 * - Discipline/fitness scores: frequency + consistency + volume trend weighting
 * - VO2 max: Jack Daniels VDOT formula from best available PR
 * - Race time predictions: Riegel's formula (t2 = t1 × (d2/d1)^1.06)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceIntelligenceService {

    private final ActivityRepository activityRepository;
    private final PersonalRecordRepository personalRecordRepository;

    /** How many weeks of history to analyse. */
    private static final int ANALYSIS_WEEKS = 8;

    /** Minimum distance (m) that counts as a "long run". */
    private static final int LONG_RUN_THRESHOLD_M = 10_000;

    /**
     * Computes and returns a full PerformanceResponse for the given user.
     * Uses 90 days of activity history to support realistic streak lengths.
     */
    @Transactional(readOnly = true)
    public PerformanceResponse compute(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        List<Activity> allRecent = activityRepository.findByUserIdSince(userId, since);

        // Most metrics are running-specific
        List<Activity> runActivities = allRecent.stream()
                .filter(a -> a.getSportType() == Activity.SportType.RUN)
                .collect(Collectors.toList());

        PersonalRecord pr = personalRecordRepository.findByUserId(userId).orElse(null);

        LocalDate today = LocalDate.now();
        LocalDate currentWeekMonday = today.with(DayOfWeek.MONDAY);

        // Bucket activities by week (0 = current week, 1 = last week, …)
        Map<Integer, List<Activity>> byWeek = buildWeekBuckets(runActivities, currentWeekMonday);

        List<PerformanceResponse.WeeklyVolumeSummary> trend = buildVolumeTrend(byWeek, currentWeekMonday);

        // Current-week and average volumes (exclude partial current week from average)
        int currentWeekVol = sumMeters(byWeek.getOrDefault(0, List.of()));
        int avgWeeklyVol = (int) trend.stream()
                .limit(ANALYSIS_WEEKS - 1)  // oldest 7 completed weeks
                .mapToInt(PerformanceResponse.WeeklyVolumeSummary::getMeters)
                .average()
                .orElse(0);

        double consistencyPct = computeConsistencyPct(byWeek);
        double avgRunsPerWeek = computeAvgRunsPerWeek(byWeek);
        boolean hasRecentLongRun = hasLongRunInLastNWeeks(byWeek, 2);
        boolean improving = isVolumeImproving(trend);

        int disciplineScore = computeDisciplineScore(consistencyPct, avgRunsPerWeek, hasRecentLongRun, improving);
        String disciplineLevel = computeDisciplineLevel(disciplineScore);
        Double vo2max = estimateVo2max(pr);
        Map<String, String> predictions = computeRacePredictions(pr);
        List<String> gaps = computeTrainingGaps(byWeek, currentWeekVol, avgWeeklyVol, consistencyPct);
        int streak = computeCurrentStreak(allRecent);
        int fitnessScore = computeFitnessScore(disciplineScore, currentWeekVol, avgWeeklyVol, vo2max);

        log.debug("Performance computed: userId={} fitness={} discipline={} vo2max={} streak={}",
                userId, fitnessScore, disciplineScore, vo2max, streak);

        PerformanceResponse response = new PerformanceResponse();
        response.setFitnessScore(fitnessScore);
        response.setDisciplineScore(disciplineScore);
        response.setDisciplineLevel(disciplineLevel);
        response.setTrainingConsistency(Math.round(consistencyPct * 10) / 10.0);
        response.setCurrentWeekVolumeMeters(currentWeekVol);
        response.setAvgWeeklyVolumeMeters(avgWeeklyVol);
        response.setWeeklyVolumeTrend(trend);
        response.setVo2maxEstimate(vo2max != null ? Math.round(vo2max * 10) / 10.0 : null);
        response.setPredictedRaceTimes(predictions);
        response.setTrainingGaps(gaps);
        response.setCurrentStreakDays(streak);
        return response;
    }

    // ── Week bucketing ────────────────────────────────────────────────────────

    /**
     * Assigns each activity to a week bucket where 0 = current week,
     * 1 = last week, … up to ANALYSIS_WEEKS - 1.
     */
    private Map<Integer, List<Activity>> buildWeekBuckets(
            List<Activity> activities, LocalDate currentWeekMonday) {
        Map<Integer, List<Activity>> buckets = new HashMap<>();
        for (Activity a : activities) {
            LocalDate actMonday = a.getCreatedAt().toLocalDate().with(DayOfWeek.MONDAY);
            int weeksAgo = (int) ChronoUnit.WEEKS.between(actMonday, currentWeekMonday);
            if (weeksAgo >= 0 && weeksAgo < ANALYSIS_WEEKS) {
                buckets.computeIfAbsent(weeksAgo, k -> new ArrayList<>()).add(a);
            }
        }
        return buckets;
    }

    /**
     * Builds a list of WeeklyVolumeSummary ordered oldest → most-recent
     * (suitable for chart rendering).
     */
    private List<PerformanceResponse.WeeklyVolumeSummary> buildVolumeTrend(
            Map<Integer, List<Activity>> byWeek, LocalDate currentWeekMonday) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        List<PerformanceResponse.WeeklyVolumeSummary> trend = new ArrayList<>();
        // Iterate from oldest (ANALYSIS_WEEKS-1 weeks ago) to current (0 weeks ago)
        for (int i = ANALYSIS_WEEKS - 1; i >= 0; i--) {
            LocalDate wStart = currentWeekMonday.minusWeeks(i);
            LocalDate wEnd = wStart.plusDays(6);
            List<Activity> weekActs = byWeek.getOrDefault(i, List.of());
            String label = wStart.format(fmt) + " – " + wEnd.format(fmt);
            trend.add(new PerformanceResponse.WeeklyVolumeSummary(
                    label, sumMeters(weekActs), weekActs.size()));
        }
        return trend;
    }

    // ── Consistency & frequency ───────────────────────────────────────────────

    private double computeConsistencyPct(Map<Integer, List<Activity>> byWeek) {
        long weeksWithRuns = 0;
        for (int i = 0; i < ANALYSIS_WEEKS; i++) {
            if (!byWeek.getOrDefault(i, List.of()).isEmpty()) weeksWithRuns++;
        }
        return (weeksWithRuns * 100.0) / ANALYSIS_WEEKS;
    }

    private double computeAvgRunsPerWeek(Map<Integer, List<Activity>> byWeek) {
        int total = byWeek.values().stream().mapToInt(List::size).sum();
        return total / (double) ANALYSIS_WEEKS;
    }

    private boolean hasLongRunInLastNWeeks(Map<Integer, List<Activity>> byWeek, int n) {
        for (int i = 0; i < n; i++) {
            boolean found = byWeek.getOrDefault(i, List.of()).stream()
                    .anyMatch(a -> a.getDistanceMeters() != null
                            && a.getDistanceMeters() >= LONG_RUN_THRESHOLD_M);
            if (found) return true;
        }
        return false;
    }

    /**
     * Returns true if the most-recent 4 weeks' average volume is at least 95%
     * of the previous 4 weeks' average (stable or improving trend).
     */
    private boolean isVolumeImproving(List<PerformanceResponse.WeeklyVolumeSummary> trend) {
        if (trend.size() < ANALYSIS_WEEKS) return false;
        // trend is oldest-first; last 4 entries = most recent
        double recentAvg = trend.subList(ANALYSIS_WEEKS - 4, ANALYSIS_WEEKS).stream()
                .mapToInt(PerformanceResponse.WeeklyVolumeSummary::getMeters).average().orElse(0);
        double previousAvg = trend.subList(0, 4).stream()
                .mapToInt(PerformanceResponse.WeeklyVolumeSummary::getMeters).average().orElse(0);
        return recentAvg >= previousAvg * 0.95;
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    /**
     * Discipline score weights:
     *   40% run frequency (normalised to 6 runs/week max)
     *   30% consistency (% weeks with ≥1 run)
     *   15% long-run presence in last 2 weeks
     *   15% volume trend (stable/improving = 100, declining = 40)
     */
    private int computeDisciplineScore(double consistencyPct, double avgRunsPerWeek,
                                       boolean hasLongRun, boolean improving) {
        double freqScore = Math.min(avgRunsPerWeek / 6.0, 1.0) * 100;
        double longRunScore = hasLongRun ? 100.0 : 0.0;
        double trendScore = improving ? 100.0 : 40.0;
        double raw = 0.40 * freqScore + 0.30 * consistencyPct
                + 0.15 * longRunScore + 0.15 * trendScore;
        return (int) Math.min(Math.round(raw), 100);
    }

    private String computeDisciplineLevel(int score) {
        if (score >= 80) return "ELITE";
        if (score >= 60) return "DISCIPLINED";
        if (score >= 35) return "CONSISTENT";
        return "BEGINNER";
    }

    /**
     * Fitness score weights:
     *   40% discipline score
     *   35% current-week volume vs 7-week average (volume ratio, capped at 1.5×)
     *   25% VO2 max normalised 30–80 mL/kg/min → 0–100 (falls back to discipline)
     */
    private int computeFitnessScore(int disciplineScore, int currentWeekVol,
                                    int avgWeeklyVol, Double vo2max) {
        double volumeRatio = avgWeeklyVol > 0
                ? Math.min(currentWeekVol / (double) avgWeeklyVol, 1.5)
                : 0.5;
        double volumeScore = Math.min(volumeRatio / 1.5 * 100, 100);
        double vo2Score = vo2max != null
                ? Math.min(Math.max((vo2max - 30) / 50.0 * 100, 0), 100)
                : disciplineScore;
        return (int) Math.min(Math.round(0.40 * disciplineScore + 0.35 * volumeScore + 0.25 * vo2Score), 100);
    }

    // ── VO2 max (Jack Daniels VDOT) ───────────────────────────────────────────

    /**
     * Estimates VO2 max using Jack Daniels' oxygen-cost formula:
     *   VO2 at pace = -4.60 + 0.182258v + 0.000104v²   (v in m/min)
     * Divided by fractional utilization at that distance:
     *   5K → 0.985 | 10K → 0.952 | half marathon → 0.901
     * Returns the highest estimate across available PRs.
     */
    private Double estimateVo2max(PersonalRecord pr) {
        if (pr == null) return null;
        Double best = null;

        if (pr.getBest5k() != null && pr.getBest5k() > 0) {
            double v = 5000.0 / (pr.getBest5k() / 60.0);
            double vo2 = danielsOxygen(v) / 0.985;
            best = best == null ? vo2 : Math.max(best, vo2);
        }
        if (pr.getBest10k() != null && pr.getBest10k() > 0) {
            double v = 10000.0 / (pr.getBest10k() / 60.0);
            double vo2 = danielsOxygen(v) / 0.952;
            best = best == null ? vo2 : Math.max(best, vo2);
        }
        if (pr.getBestHalf() != null && pr.getBestHalf() > 0) {
            double v = 21097.5 / (pr.getBestHalf() / 60.0);
            double vo2 = danielsOxygen(v) / 0.901;
            best = best == null ? vo2 : Math.max(best, vo2);
        }
        return (best != null && best > 20) ? best : null;
    }

    /** Jack Daniels' oxygen cost of running at v m/min: -4.60 + 0.182258v + 0.000104v² */
    private double danielsOxygen(double v) {
        return -4.60 + 0.182258 * v + 0.000104 * v * v;
    }

    // ── Race time prediction (Riegel) ─────────────────────────────────────────

    /**
     * Predicts race times using Pete Riegel's formula: t2 = t1 × (d2/d1)^1.06.
     * Uses the best available PR as the reference effort (prefers shorter distances
     * as they produce more accurate extrapolations).
     */
    private Map<String, String> computeRacePredictions(PersonalRecord pr) {
        if (pr == null) return Map.of();

        double refTime = 0, refDist = 0;
        if (pr.getBest5k() != null && pr.getBest5k() > 0) {
            refTime = pr.getBest5k(); refDist = 5_000;
        } else if (pr.getBest10k() != null && pr.getBest10k() > 0) {
            refTime = pr.getBest10k(); refDist = 10_000;
        } else if (pr.getBestHalf() != null && pr.getBestHalf() > 0) {
            refTime = pr.getBestHalf(); refDist = 21_097.5;
        }
        if (refTime == 0) return Map.of();

        final double t1 = refTime, d1 = refDist;
        Map<String, String> predictions = new LinkedHashMap<>();
        predictions.put("5K",          formatSeconds(riegel(t1, d1, 5_000)));
        predictions.put("10K",         formatSeconds(riegel(t1, d1, 10_000)));
        predictions.put("halfMarathon", formatSeconds(riegel(t1, d1, 21_097.5)));
        predictions.put("marathon",    formatSeconds(riegel(t1, d1, 42_195)));
        return predictions;
    }

    private double riegel(double t1, double d1, double d2) {
        return t1 * Math.pow(d2 / d1, 1.06);
    }

    private String formatSeconds(double totalSeconds) {
        long secs = Math.round(totalSeconds);
        long hours = secs / 3600;
        long mins = (secs % 3600) / 60;
        long seconds = secs % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, mins, seconds)
                : String.format("%d:%02d", mins, seconds);
    }

    // ── Training gap detection ────────────────────────────────────────────────

    private List<String> computeTrainingGaps(Map<Integer, List<Activity>> byWeek,
                                              int currentWeekVol, int avgWeeklyVol,
                                              double consistencyPct) {
        List<String> gaps = new ArrayList<>();

        // No runs logged this week
        boolean ranThisWeek = !byWeek.getOrDefault(0, List.of()).isEmpty();
        boolean ranLastWeek = !byWeek.getOrDefault(1, List.of()).isEmpty();
        if (!ranThisWeek) {
            gaps.add(ranLastWeek
                    ? "No runs logged yet this week — keep the momentum going."
                    : "No runs in the last 2 weeks — time to get back out there.");
        }

        // No long run in last 14 days
        if (ranThisWeek && !hasLongRunInLastNWeeks(byWeek, 2)) {
            gaps.add("No long run (10K+) in the last 14 days — add one to your week.");
        }

        // Volume significantly lower than usual
        if (avgWeeklyVol > 0 && currentWeekVol > 0 && currentWeekVol < avgWeeklyVol * 0.5) {
            gaps.add("Volume is lower than usual this week — try to close the gap.");
        }

        // Low overall consistency across 8 weeks
        if (consistencyPct < 50 && !byWeek.values().isEmpty()) {
            gaps.add("Training consistency is below 50% over the last 8 weeks — aim for at least 3 weeks on, 1 week recovery.");
        }

        return gaps;
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    /** Counts consecutive calendar days (ending today) with ≥ 1 activity of any sport. */
    private int computeCurrentStreak(List<Activity> activities) {
        if (activities.isEmpty()) return 0;
        Set<LocalDate> activeDays = activities.stream()
                .map(a -> a.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate day = LocalDate.now();
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private int sumMeters(List<Activity> activities) {
        return activities.stream()
                .mapToInt(a -> a.getDistanceMeters() != null ? a.getDistanceMeters() : 0)
                .sum();
    }
}
