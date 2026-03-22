package com.runnit.api.service;

import com.runnit.api.dto.TrainingBlockResponse;
import com.runnit.api.dto.TrainingBlockResponse.PhaseInfo;
import com.runnit.api.dto.TrainingBlockResponse.WeekVolume;
import com.runnit.api.dto.TrainingBlockResponse.WorkoutSummary;
import com.runnit.api.model.Plan;
import com.runnit.api.model.PlanWorkout;
import com.runnit.api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingBlockService {

    private final PlanRepository planRepository;

    /**
     * Builds a full TrainingBlockResponse for the user's currently active plan.
     *
     * @param userId the authenticated user's ID
     * @return populated TrainingBlockResponse, or null if no active plan exists
     */
    @Transactional(readOnly = true)
    public TrainingBlockResponse getBlockSummary(Long userId) {
        Plan plan = planRepository.findByUserIdAndActiveTrue(userId).orElse(null);
        if (plan == null) {
            log.debug("No active plan found for userId={}", userId);
            return null;
        }

        int totalWeeks = plan.getTotalWeeks() != null ? plan.getTotalWeeks() : 1;
        int currentWeek = resolveCurrentWeek(plan, totalWeeks);

        TrainingBlockResponse response = new TrainingBlockResponse();
        response.setCurrentPhase(computePhase(currentWeek, totalWeeks));
        response.setCurrentTheme(computeTheme(currentWeek, totalWeeks));
        response.setCurrentWeek(currentWeek);
        response.setTotalWeeks(totalWeeks);
        response.setDaysToRace(computeDaysToRace(plan));
        response.setOverallProgress(computeOverallProgress(currentWeek, totalWeeks));
        response.setPhases(buildPhaseBoundaries(totalWeeks));
        response.setWeeklyVolume(buildWeeklyVolume(plan, totalWeeks));
        response.setCurrentWeekWorkouts(buildCurrentWeekWorkouts(plan, currentWeek));

        return response;
    }

    // ── Current Week Resolution ───────────────────────────────────────────────

    /**
     * Derives the current week number from startDate if available,
     * otherwise falls back to week 1 (plan may not have started yet).
     * Result is clamped to [1, totalWeeks].
     */
    private int resolveCurrentWeek(Plan plan, int totalWeeks) {
        if (plan.getStartDate() != null) {
            long weeksSinceStart = ChronoUnit.WEEKS.between(plan.getStartDate(), LocalDate.now());
            int week = (int) weeksSinceStart + 1; // 1-based
            return Math.max(1, Math.min(week, totalWeeks));
        }
        return 1;
    }

    // ── Phase & Theme ─────────────────────────────────────────────────────────

    private String computePhase(int weekNumber, int totalWeeks) {
        int baseEnd = (int) Math.ceil(totalWeeks * 0.40);
        int buildEnd = baseEnd + (int) Math.ceil(totalWeeks * 0.35);
        int peakEnd  = buildEnd + (int) Math.ceil(totalWeeks * 0.15);
        if (weekNumber <= baseEnd)  return "BASE";
        if (weekNumber <= buildEnd) return "BUILD";
        if (weekNumber <= peakEnd)  return "PEAK";
        return "TAPER";
    }

    private String computeTheme(int weekNumber, int totalWeeks) {
        return switch (computePhase(weekNumber, totalWeeks)) {
            case "BASE"  -> weekNumber % 4 == 0 ? "Recovery" : "Base Building";
            case "BUILD" -> weekNumber % 4 == 0 ? "Recovery" : "Build Phase";
            case "PEAK"  -> "Peak Week";
            case "TAPER" -> weekNumber == totalWeeks ? "Race Week" : "Taper";
            default      -> "Training Week";
        };
    }

    // ── Phase Boundaries ──────────────────────────────────────────────────────

    /**
     * Computes start/end week for each of the 4 phases based on totalWeeks ratios.
     * BASE 40% → BUILD 35% → PEAK 15% → TAPER remainder.
     */
    private List<PhaseInfo> buildPhaseBoundaries(int totalWeeks) {
        int baseEnd  = (int) Math.ceil(totalWeeks * 0.40);
        int buildEnd = baseEnd + (int) Math.ceil(totalWeeks * 0.35);
        int peakEnd  = buildEnd + (int) Math.ceil(totalWeeks * 0.15);
        int taperEnd = totalWeeks;

        List<PhaseInfo> phases = new ArrayList<>();
        phases.add(new PhaseInfo("BASE",  1,           baseEnd));
        phases.add(new PhaseInfo("BUILD", baseEnd + 1, buildEnd));
        phases.add(new PhaseInfo("PEAK",  buildEnd + 1, peakEnd));
        phases.add(new PhaseInfo("TAPER", peakEnd + 1, taperEnd));
        return phases;
    }

    // ── Days To Race ─────────────────────────────────────────────────────────

    private Integer computeDaysToRace(Plan plan) {
        if (plan.getTargetRaceDate() == null) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), plan.getTargetRaceDate());
        return (int) Math.max(0, days);
    }

    // ── Overall Progress ─────────────────────────────────────────────────────

    /** Returns a rounded percentage (0.0–100.0) of how far through the plan we are. */
    private double computeOverallProgress(int currentWeek, int totalWeeks) {
        double raw = ((currentWeek - 1.0) / totalWeeks) * 100.0;
        return Math.round(raw * 10) / 10.0;
    }

    // ── Weekly Volume ─────────────────────────────────────────────────────────

    /**
     * Aggregates plannedMeters and completedMeters per week across all plan workouts.
     * Weeks with no workouts are still included for continuity.
     */
    private List<WeekVolume> buildWeeklyVolume(Plan plan, int totalWeeks) {
        List<PlanWorkout> workouts = plan.getWorkouts();

        // Group by weekNumber, falling back to day-based derivation for legacy data
        Map<Integer, List<PlanWorkout>> byWeek = (workouts != null ? workouts : List.<PlanWorkout>of())
                .stream()
                .collect(Collectors.groupingBy(w -> w.getWeekNumber() != null
                        ? w.getWeekNumber()
                        : (w.getDay() != null ? ((w.getDay() - 1) / 7) + 1 : 1)));

        List<WeekVolume> volumes = new ArrayList<>();
        for (int wn = 1; wn <= totalWeeks; wn++) {
            List<PlanWorkout> weekWorkouts = byWeek.getOrDefault(wn, List.of());
            int planned   = weekWorkouts.stream()
                    .mapToInt(w -> w.getDistanceMeters() != null ? w.getDistanceMeters() : 0)
                    .sum();
            int completed = weekWorkouts.stream()
                    .filter(PlanWorkout::isCompleted)
                    .mapToInt(w -> w.getDistanceMeters() != null ? w.getDistanceMeters() : 0)
                    .sum();
            volumes.add(new WeekVolume(wn, planned, completed));
        }
        return volumes;
    }

    // ── Current Week Workouts ─────────────────────────────────────────────────

    private List<WorkoutSummary> buildCurrentWeekWorkouts(Plan plan, int currentWeek) {
        List<PlanWorkout> workouts = plan.getWorkouts();
        if (workouts == null) return List.of();

        return workouts.stream()
                .filter(w -> {
                    int wn = w.getWeekNumber() != null
                            ? w.getWeekNumber()
                            : (w.getDay() != null ? ((w.getDay() - 1) / 7) + 1 : 1);
                    return wn == currentWeek;
                })
                .sorted(Comparator.comparingInt(w -> w.getDay() != null ? w.getDay() : 0))
                .map(this::toWorkoutSummary)
                .collect(Collectors.toList());
    }

    private WorkoutSummary toWorkoutSummary(PlanWorkout w) {
        WorkoutSummary ws = new WorkoutSummary();
        ws.setId(w.getId());
        ws.setDayLabel(w.getTitle());
        ws.setWorkoutType(w.getWorkoutType());
        ws.setType(mapWorkoutType(w.getWorkoutType()));
        ws.setDescription(w.getDescription());
        ws.setDurationMinutes(w.getDurationMinutes());
        ws.setDistanceMeters(w.getDistanceMeters());
        ws.setTargetPaceSeconds(w.getTargetPaceSeconds());
        ws.setCompleted(w.isCompleted());
        return ws;
    }

    private String mapWorkoutType(String workoutType) {
        if (workoutType == null) return "Easy Run";
        return switch (workoutType) {
            case "EASY"     -> "Easy Run";
            case "TEMPO"    -> "Tempo Run";
            case "INTERVAL" -> "Interval";
            case "LONG_RUN" -> "Long Run";
            case "RECOVERY" -> "Recovery Run";
            case "REST"     -> "Rest";
            default         -> workoutType;
        };
    }
}
