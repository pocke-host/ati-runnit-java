package com.runnit.api.dto;

import java.util.List;

/**
 * Response DTO for GET /api/plans/active/block.
 * Represents a full training block summary for the currently active plan.
 */
public class TrainingBlockResponse {

    /** Human-readable phase name: BASE, BUILD, PEAK, or TAPER. */
    private String currentPhase;

    /** The theme for the current week (e.g. "Base Building", "Recovery", "Race Week"). */
    private String currentTheme;

    /** 1-based current week number within the plan. */
    private int currentWeek;

    /** Total weeks in the plan. */
    private int totalWeeks;

    /** Calendar days remaining until targetRaceDate. Null if no race date set. */
    private Integer daysToRace;

    /** Percentage (0–100) of the overall plan that has elapsed. */
    private double overallProgress;

    /** Phase boundary breakdown: name, startWeek, endWeek, weekCount. */
    private List<PhaseInfo> phases;

    /**
     * Per-week volume summary across the entire plan.
     * weekNumber, plannedMeters, completedMeters, completionPct.
     */
    private List<WeekVolume> weeklyVolume;

    /** Workouts belonging to the current week. */
    private List<WorkoutSummary> currentWeekWorkouts;

    // ── Constructors ──────────────────────────────────────────────────────────

    public TrainingBlockResponse() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public String getCurrentTheme() { return currentTheme; }
    public void setCurrentTheme(String currentTheme) { this.currentTheme = currentTheme; }

    public int getCurrentWeek() { return currentWeek; }
    public void setCurrentWeek(int currentWeek) { this.currentWeek = currentWeek; }

    public int getTotalWeeks() { return totalWeeks; }
    public void setTotalWeeks(int totalWeeks) { this.totalWeeks = totalWeeks; }

    public Integer getDaysToRace() { return daysToRace; }
    public void setDaysToRace(Integer daysToRace) { this.daysToRace = daysToRace; }

    public double getOverallProgress() { return overallProgress; }
    public void setOverallProgress(double overallProgress) { this.overallProgress = overallProgress; }

    public List<PhaseInfo> getPhases() { return phases; }
    public void setPhases(List<PhaseInfo> phases) { this.phases = phases; }

    public List<WeekVolume> getWeeklyVolume() { return weeklyVolume; }
    public void setWeeklyVolume(List<WeekVolume> weeklyVolume) { this.weeklyVolume = weeklyVolume; }

    public List<WorkoutSummary> getCurrentWeekWorkouts() { return currentWeekWorkouts; }
    public void setCurrentWeekWorkouts(List<WorkoutSummary> currentWeekWorkouts) { this.currentWeekWorkouts = currentWeekWorkouts; }

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    public static class PhaseInfo {
        private String name;
        private int startWeek;
        private int endWeek;
        private int weekCount;

        public PhaseInfo(String name, int startWeek, int endWeek) {
            this.name = name;
            this.startWeek = startWeek;
            this.endWeek = endWeek;
            this.weekCount = endWeek - startWeek + 1;
        }

        public String getName() { return name; }
        public int getStartWeek() { return startWeek; }
        public int getEndWeek() { return endWeek; }
        public int getWeekCount() { return weekCount; }
    }

    public static class WeekVolume {
        private int weekNumber;
        private int plannedMeters;
        private int completedMeters;
        private double completionPct;

        public WeekVolume(int weekNumber, int plannedMeters, int completedMeters) {
            this.weekNumber = weekNumber;
            this.plannedMeters = plannedMeters;
            this.completedMeters = completedMeters;
            this.completionPct = plannedMeters > 0
                    ? Math.round((completedMeters * 100.0 / plannedMeters) * 10) / 10.0
                    : 0.0;
        }

        public int getWeekNumber() { return weekNumber; }
        public int getPlannedMeters() { return plannedMeters; }
        public int getCompletedMeters() { return completedMeters; }
        public double getCompletionPct() { return completionPct; }
    }

    public static class WorkoutSummary {
        private Long id;
        private String dayLabel;
        private String workoutType;
        private String type;
        private String description;
        private Integer durationMinutes;
        private Integer distanceMeters;
        private Integer targetPaceSeconds;
        private boolean completed;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getDayLabel() { return dayLabel; }
        public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

        public String getWorkoutType() { return workoutType; }
        public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

        public Integer getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }

        public Integer getTargetPaceSeconds() { return targetPaceSeconds; }
        public void setTargetPaceSeconds(Integer targetPaceSeconds) { this.targetPaceSeconds = targetPaceSeconds; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}
