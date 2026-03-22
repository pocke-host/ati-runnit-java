package com.runnit.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for GET /api/users/me/performance.
 * All metrics are derived from the user's existing activity and PR data —
 * no external APIs required.
 */
public class PerformanceResponse {

    // ── Overall scores ─────────────────────────────────────────────────────

    /** Composite fitness score 0–100 (volume + consistency + VO2 max). */
    private int fitnessScore;

    /** Training discipline score 0–100 (frequency + consistency + long runs + trend). */
    private int disciplineScore;

    /** Human-readable level: BEGINNER, CONSISTENT, DISCIPLINED, or ELITE. */
    private String disciplineLevel;

    /** Percentage (0–100) of the last 8 weeks that contained at least one run. */
    private double trainingConsistency;

    // ── Volume ─────────────────────────────────────────────────────────────

    /** Total running distance logged in the current (partial) week, in meters. */
    private int currentWeekVolumeMeters;

    /**
     * Average weekly running volume over the last 7 completed weeks, in meters.
     * Excludes the current partial week.
     */
    private int avgWeeklyVolumeMeters;

    /** Per-week volume breakdown for the last 8 weeks, oldest first. */
    private List<WeeklyVolumeSummary> weeklyVolumeTrend;

    // ── Fitness estimates ──────────────────────────────────────────────────

    /**
     * VO2 max estimate in mL/kg/min, derived from PR times via Jack Daniels'
     * VDOT formula. Null when no PR data is available.
     */
    private Double vo2maxEstimate;

    /**
     * Riegel-formula race time predictions keyed by distance name:
     * "5K", "10K", "halfMarathon", "marathon".
     * Empty map if no PR data is available.
     */
    private Map<String, String> predictedRaceTimes;

    // ── Insights ───────────────────────────────────────────────────────────

    /**
     * Actionable training gap messages, e.g.:
     * "No long run (10K+) in the last 14 days — add one this week."
     */
    private List<String> trainingGaps;

    /** Number of consecutive days (ending today) with at least one activity logged. */
    private int currentStreakDays;

    // ── Constructors ───────────────────────────────────────────────────────

    public PerformanceResponse() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(int fitnessScore) { this.fitnessScore = fitnessScore; }

    public int getDisciplineScore() { return disciplineScore; }
    public void setDisciplineScore(int disciplineScore) { this.disciplineScore = disciplineScore; }

    public String getDisciplineLevel() { return disciplineLevel; }
    public void setDisciplineLevel(String disciplineLevel) { this.disciplineLevel = disciplineLevel; }

    public double getTrainingConsistency() { return trainingConsistency; }
    public void setTrainingConsistency(double trainingConsistency) { this.trainingConsistency = trainingConsistency; }

    public int getCurrentWeekVolumeMeters() { return currentWeekVolumeMeters; }
    public void setCurrentWeekVolumeMeters(int currentWeekVolumeMeters) { this.currentWeekVolumeMeters = currentWeekVolumeMeters; }

    public int getAvgWeeklyVolumeMeters() { return avgWeeklyVolumeMeters; }
    public void setAvgWeeklyVolumeMeters(int avgWeeklyVolumeMeters) { this.avgWeeklyVolumeMeters = avgWeeklyVolumeMeters; }

    public List<WeeklyVolumeSummary> getWeeklyVolumeTrend() { return weeklyVolumeTrend; }
    public void setWeeklyVolumeTrend(List<WeeklyVolumeSummary> weeklyVolumeTrend) { this.weeklyVolumeTrend = weeklyVolumeTrend; }

    public Double getVo2maxEstimate() { return vo2maxEstimate; }
    public void setVo2maxEstimate(Double vo2maxEstimate) { this.vo2maxEstimate = vo2maxEstimate; }

    public Map<String, String> getPredictedRaceTimes() { return predictedRaceTimes; }
    public void setPredictedRaceTimes(Map<String, String> predictedRaceTimes) { this.predictedRaceTimes = predictedRaceTimes; }

    public List<String> getTrainingGaps() { return trainingGaps; }
    public void setTrainingGaps(List<String> trainingGaps) { this.trainingGaps = trainingGaps; }

    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }

    // ── Nested DTO ─────────────────────────────────────────────────────────

    public static class WeeklyVolumeSummary {
        /** Human-readable week label, e.g. "Mar 10 – Mar 16". */
        private String weekLabel;
        /** Total running meters logged this week. */
        private int meters;
        /** Number of runs logged this week. */
        private int runCount;

        public WeeklyVolumeSummary(String weekLabel, int meters, int runCount) {
            this.weekLabel = weekLabel;
            this.meters = meters;
            this.runCount = runCount;
        }

        public String getWeekLabel() { return weekLabel; }
        public int getMeters() { return meters; }
        public int getRunCount() { return runCount; }
    }
}
