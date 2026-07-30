package com.runnit.api.service;

import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import com.runnit.api.util.PlanDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runnit's answer to TriDot's "FitLogic" — recalculates a user's upcoming
 * training off their own completed performance and recovery data instead of
 * a fixed calendar. Deliberately a transparent rules engine, not a black box:
 * every threshold below maps to a human-readable reason, logged as a
 * PlanAdaptation row, so a future UI can always explain "why did my plan
 * change" in plain language.
 *
 * Scope, by design: only ever adjusts targetPaceSeconds/durationMinutes/
 * distanceMeters/workoutType on the next few upcoming INCOMPLETE workouts.
 * Never reorders, inserts, or deletes workouts, and never touches completed
 * ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptivePlanService {

    private final PlanRepository planRepository;
    private final PlanWorkoutRepository planWorkoutRepository;
    private final PlanAdaptationRepository planAdaptationRepository;
    private final NotificationRepository notificationRepository;
    private final WellnessDailyRepository wellnessDailyRepository;
    private final ActivityRepository activityRepository;
    private final TrainingLoadService trainingLoadService;

    // ── Tunable thresholds — every one maps to a human-readable reason string ──

    /** Activities older than this are historical backfill noise, not "just happened" signal. */
    private static final int BACKFILL_CUTOFF_HOURS = 48;

    private static final double ACWR_HIGH_RISK = 1.5;      // matches TrainingLoadService's own HIGH_RISK cutoff
    private static final double ACWR_MODERATE_RISK = 1.3;  // softer intervention band below hard risk
    private static final double TSB_VERY_NEGATIVE = -20.0; // deep fatigue / overreaching risk
    private static final int RECOVERY_LOW = 33;            // WHOOP's own red-zone recovery cutoff (0-100)

    private static final double SOFTEN_PCT_MODERATE = 0.08;
    private static final double SOFTEN_PCT_HIGH = 0.15;
    private static final double PROGRESSION_TIGHTEN_PCT = 0.05; // smaller than softening — regress generously, progress conservatively

    private static final int MAX_WORKOUTS_ADAPTED_PER_EVENT = 3;
    private static final int PROGRESSION_STREAK_REQUIRED = 3;
    private static final double PROGRESSION_PACE_MARGIN = 0.05; // must be ≥5% faster than prescribed to count

    private static final Set<String> HARD_TYPES = Set.of("TEMPO", "INTERVAL");
    private static final Set<String> HARD_OR_LONG_TYPES = Set.of("TEMPO", "INTERVAL", "LONG_RUN");

    // ── Entry points ────────────────────────────────────────────────────────

    /** Called from every activity-save site. Never allowed to throw into the caller. */
    @Transactional
    public void onActivityRecorded(Activity activity) {
        if (activity == null || activity.getUser() == null) return;
        Long userId = activity.getUser().getId();

        LocalDateTime performedAt = activity.getPerformedAt() != null ? activity.getPerformedAt() : activity.getCreatedAt();
        if (performedAt == null || performedAt.isBefore(LocalDateTime.now().minusHours(BACKFILL_CUTOFF_HOURS))) {
            return; // stale/backfill activity — irrelevant to today's/tomorrow's plan, and cheap to skip before any DB work
        }

        Plan plan = planRepository.findByUserIdAndActiveTrue(userId).orElse(null);
        if (plan == null) return; // no active plan — legitimate no-op

        linkActivityToWorkout(plan, activity, performedAt.toLocalDate());
        evaluateAndAdapt(userId, plan, activity, "ACTIVITY");
    }

    @Transactional
    public void runNightlySweepForAllActivePlans() {
        for (Plan plan : planRepository.findAllByActiveTrue()) {
            Long userId = plan.getUser() != null ? plan.getUser().getId() : null;
            if (userId == null) continue;
            try {
                softenForMissedWorkouts(plan);
                evaluateAndAdapt(userId, plan, null, "SCHEDULED");
            } catch (Exception e) {
                log.warn("Nightly adaptive sweep failed for userId={}: {}", userId, e.getMessage());
            }
        }
    }

    // ── Linking an activity to a workout ───────────────────────────────────

    /**
     * PlanWorkout.day is an ordinal slot within the week (1st/2nd/3rd workout),
     * not a literal weekday — plan-creation wizards assign it as a simple loop
     * index. So matching is by calendar WEEK (via PlanDateUtil), not by day.
     * No match is a legitimate outcome (rest day / off-plan activity) — the
     * caller still proceeds to adaptation evaluation, since effort still
     * affects ACWR regardless of whether it landed on a plan slot.
     */
    private void linkActivityToWorkout(Plan plan, Activity activity, LocalDate activityDate) {
        Integer targetWeek = PlanDateUtil.weekNumberContaining(plan, activityDate);
        if (targetWeek == null) return;

        List<PlanWorkout> candidates = planWorkoutRepository
                .findByPlanIdAndCompletedFalseOrderByWeekNumberAscDayAsc(plan.getId());

        PlanWorkout match = candidates.stream()
                .filter(w -> targetWeek.equals(w.getWeekNumber()))
                .min(Comparator.comparingInt(w -> sportMismatchPenalty(plan, activity, w)))
                .orElse(null);
        if (match == null) return;

        if (match.getLinkedActivityId() == null) {
            match.setLinkedActivityId(activity.getId());
        }
        match.setCompleted(true);
        planWorkoutRepository.save(match);
    }

    /** 0 = sport matches (or either side is unknown), 1 = mismatch — a soft preference, never a hard exclusion. */
    private int sportMismatchPenalty(Plan plan, Activity activity, PlanWorkout w) {
        if (plan.getSport() == null || activity.getSportType() == null) return 0;
        return plan.getSport().equalsIgnoreCase(activity.getSportType().name()) ? 0 : 1;
    }

    // ── Missed-workout softening (scheduler-only — the activity-triggered path structurally can't see this) ──

    private void softenForMissedWorkouts(Plan plan) {
        List<PlanWorkout> incomplete = planWorkoutRepository
                .findByPlanIdAndCompletedFalseOrderByWeekNumberAscDayAsc(plan.getId());
        if (incomplete.isEmpty()) return;

        LocalDate today = LocalDate.now();
        boolean hasMissed = incomplete.stream().anyMatch(w -> {
            LocalDate weekStart = PlanDateUtil.weekStartDate(plan, w.getWeekNumber());
            return weekStart != null && weekStart.plusDays(6).isBefore(today);
        });
        if (!hasMissed) return;

        PlanWorkout next = incomplete.stream()
                .filter(w -> !alreadyAdaptedToday(w))
                .filter(w -> !"REST".equals(w.getWorkoutType()) && !"EASY".equals(w.getWorkoutType()))
                .findFirst().orElse(null);
        if (next == null) return;

        String reason = String.format(
                "You missed a scheduled workout this week. We eased your next %s to help you ease back in.",
                displayType(next.getWorkoutType()));
        applyDecision(plan, next, null, "SCHEDULED",
                Decision.softenOnly(SOFTEN_PCT_MODERATE, reason));
    }

    private boolean alreadyAdaptedToday(PlanWorkout w) {
        return w.getAdaptedAt() != null && w.getAdaptedAt().toLocalDate().equals(LocalDate.now());
    }

    // ── Core rule evaluation (R1-R5) ───────────────────────────────────────

    private void evaluateAndAdapt(Long userId, Plan plan, Activity trigger, String triggeredBy) {
        Map<String, Object> load = trainingLoadService.computeMetrics(userId);
        double acwr = ((Number) load.get("acwr")).doubleValue();
        double tsb = ((Number) load.get("tsb")).doubleValue();
        String riskLabel = (String) load.get("riskLabel");
        boolean hasEnoughData = Boolean.TRUE.equals(load.get("hasEnoughData"));

        Integer recoveryScore = wellnessDailyRepository.findByUserIdAndDate(userId, LocalDate.now())
                .map(WellnessDaily::getRecoveryScore).orElse(null);

        List<PlanWorkout> upcoming = planWorkoutRepository
                .findByPlanIdAndCompletedFalseOrderByWeekNumberAscDayAsc(plan.getId());
        if (upcoming.isEmpty()) return;

        boolean recentlyProgressing = hasRecentFastCompletions(plan);

        int adapted = 0;
        for (PlanWorkout workout : upcoming) {
            if (adapted >= MAX_WORKOUTS_ADAPTED_PER_EVENT) break;
            if (trigger != null && planAdaptationRepository
                    .existsByTriggerActivityIdAndPlanWorkoutId(trigger.getId(), workout.getId())) {
                continue; // idempotency guard — already adapted this workout for this exact activity
            }

            Decision decision = decide(acwr, tsb, recoveryScore, riskLabel, hasEnoughData,
                    workout.getWorkoutType(), recentlyProgressing);
            if (decision == null) continue;

            applyDecision(plan, workout, trigger, triggeredBy, decision);
            adapted++;
        }

        if (adapted > 0) {
            notifyPlanAdapted(plan, adapted);
        }
    }

    /**
     * Pure decision logic — no I/O, package-private + static so it's directly
     * unit-testable without a Spring context. First-match-wins per workout
     * (no stacking), so the reason string always maps to exactly one cause.
     */
    static Decision decide(double acwr, double tsb, Integer recoveryScore, String riskLabel,
                            boolean hasEnoughData, String workoutType, boolean recentlyProgressing) {

        // R1 — hard risk: protect the very next hard session
        if (acwr > ACWR_HIGH_RISK && HARD_TYPES.contains(workoutType)) {
            String reason = String.format(
                    "Your acute:chronic workload ratio is %.2f (>1.5, high injury risk). We softened your next %s workout to an easy effort and reduced volume by 15%%.",
                    acwr, displayType(workoutType));
            return Decision.downgradeAndSoften("EASY", SOFTEN_PCT_HIGH, reason);
        }

        // R2 — deep fatigue + poor recovery (two independent negative signals, one step lower than R1)
        if (tsb < TSB_VERY_NEGATIVE && recoveryScore != null && recoveryScore < RECOVERY_LOW
                && HARD_OR_LONG_TYPES.contains(workoutType)) {
            String reason = String.format(
                    "Your training stress balance is %.1f and today's recovery score is %d%% (low). We downgraded your next %s to a recovery effort.",
                    tsb, recoveryScore, displayType(workoutType));
            return Decision.downgradeAndSoften("RECOVERY", SOFTEN_PCT_HIGH, reason);
        }

        // R3 — moderate risk: soften, don't replace
        if (acwr > ACWR_MODERATE_RISK && acwr <= ACWR_HIGH_RISK && HARD_TYPES.contains(workoutType)) {
            String reason = String.format(
                    "Your acute:chronic workload ratio is %.2f, trending toward overload. We eased the intensity of your next %s by 8%%.",
                    acwr, displayType(workoutType));
            return Decision.softenOnly(SOFTEN_PCT_MODERATE, reason);
        }

        // R4 — detraining safety valve: explicit no-op. Low ACWR after taper/rest isn't
        // "spare capacity" to spend — never progress on top of it. Documented, not silent.
        if ("DETRAINING".equals(riskLabel)) {
            return null;
        }

        // R5 — positive progression: the only intensity-INCREASE rule, deliberately conservative
        if (recentlyProgressing && hasEnoughData && "OPTIMAL".equals(riskLabel) && HARD_TYPES.contains(workoutType)) {
            String reason = String.format(
                    "You've completed your last %d workouts noticeably faster than prescribed pace, and your training load is in a healthy range. We tightened your next %s target pace by 5%%.",
                    PROGRESSION_STREAK_REQUIRED, displayType(workoutType));
            return Decision.tightenPaceOnly(PROGRESSION_TIGHTEN_PCT, reason);
        }

        return null;
    }

    /**
     * Last 3 linked-and-completed workouts, each run meaningfully faster than
     * its own prescribed pace. Activity.averagePace is speed in m/s (Garmin/
     * manual convention); PlanWorkout.targetPaceSeconds is seconds-per-km
     * (confirmed against PlanDetail.vue's formatPace(sPerKm)) — these are
     * different units and must be converted before comparing, not compared
     * raw.
     */
    private boolean hasRecentFastCompletions(Plan plan) {
        List<PlanWorkout> recent = planWorkoutRepository
                .findTop3ByPlanIdAndCompletedTrueAndLinkedActivityIdIsNotNullOrderByWeekNumberDescDayDesc(plan.getId());
        if (recent.size() < PROGRESSION_STREAK_REQUIRED) return false;

        for (PlanWorkout w : recent) {
            if (w.getTargetPaceSeconds() == null || w.getTargetPaceSeconds() <= 0) return false;
            Activity linked = activityRepository.findById(w.getLinkedActivityId()).orElse(null);
            if (linked == null || linked.getAveragePace() == null || linked.getAveragePace() <= 0) return false;

            double actualSecPerKm = 1000.0 / linked.getAveragePace(); // m/s -> sec/km
            double fastEnoughThreshold = w.getTargetPaceSeconds() * (1 - PROGRESSION_PACE_MARGIN);
            if (actualSecPerKm >= fastEnoughThreshold) return false; // not consistently fast enough
        }
        return true;
    }

    // ── Applying a decision ────────────────────────────────────────────────

    private void applyDecision(Plan plan, PlanWorkout workout, Activity trigger, String triggeredBy, Decision d) {
        preserveOriginalIfFirstAdaptation(workout);
        workout.setAdaptedAt(LocalDateTime.now());

        if (d.newWorkoutType() != null && !d.newWorkoutType().equals(workout.getWorkoutType())) {
            String old = workout.getWorkoutType();
            workout.setWorkoutType(d.newWorkoutType());
            recordAdaptation(plan, workout, trigger, triggeredBy, "workoutType", old, d.newWorkoutType(), d.reason());
        }
        if (d.durationDistanceFactor() != 1.0) {
            if (workout.getDurationMinutes() != null) {
                int oldV = workout.getDurationMinutes();
                int newV = Math.max(1, (int) Math.round(oldV * d.durationDistanceFactor()));
                workout.setDurationMinutes(newV);
                recordAdaptation(plan, workout, trigger, triggeredBy, "durationMinutes", String.valueOf(oldV), String.valueOf(newV), d.reason());
            }
            if (workout.getDistanceMeters() != null) {
                int oldV = workout.getDistanceMeters();
                int newV = Math.max(1, (int) Math.round(oldV * d.durationDistanceFactor()));
                workout.setDistanceMeters(newV);
                recordAdaptation(plan, workout, trigger, triggeredBy, "distanceMeters", String.valueOf(oldV), String.valueOf(newV), d.reason());
            }
        }
        if (d.paceFactor() != 1.0 && workout.getTargetPaceSeconds() != null) {
            int oldV = workout.getTargetPaceSeconds();
            int newV = Math.max(1, (int) Math.round(oldV * d.paceFactor()));
            workout.setTargetPaceSeconds(newV);
            recordAdaptation(plan, workout, trigger, triggeredBy, "targetPaceSeconds", String.valueOf(oldV), String.valueOf(newV), d.reason());
        }

        planWorkoutRepository.save(workout);
    }

    private void preserveOriginalIfFirstAdaptation(PlanWorkout workout) {
        if (workout.getAdaptedAt() == null) {
            workout.setOriginalTargetPaceSeconds(workout.getTargetPaceSeconds());
            workout.setOriginalDurationMinutes(workout.getDurationMinutes());
            workout.setOriginalDistanceMeters(workout.getDistanceMeters());
            workout.setOriginalWorkoutType(workout.getWorkoutType());
        }
    }

    private void recordAdaptation(Plan plan, PlanWorkout workout, Activity trigger, String triggeredBy,
                                   String field, String oldValue, String newValue, String reason) {
        planAdaptationRepository.save(PlanAdaptation.builder()
                .planId(plan.getId())
                .planWorkoutId(workout.getId())
                .userId(plan.getUser().getId())
                .triggeredBy(triggeredBy)
                .triggerActivityId(trigger != null ? trigger.getId() : null)
                .reason(reason)
                .fieldChanged(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    private void notifyPlanAdapted(Plan plan, int workoutsAdapted) {
        String summary = workoutsAdapted == 1
                ? "We adjusted your next workout based on your recent training load."
                : "We adjusted " + workoutsAdapted + " upcoming workouts based on your recent training load.";
        notificationRepository.save(Notification.builder()
                .user(plan.getUser())
                .type("PLAN_ADAPTED")
                .message(summary)
                .actor(null)
                .referenceId(plan.getId())
                .referenceType("PLAN")
                .build());
    }

    private static String displayType(String workoutType) {
        if (workoutType == null) return "workout";
        return switch (workoutType) {
            case "EASY" -> "easy run";
            case "TEMPO" -> "tempo run";
            case "INTERVAL" -> "interval";
            case "LONG_RUN" -> "long run";
            case "RECOVERY" -> "recovery run";
            default -> workoutType.toLowerCase().replace('_', ' ');
        };
    }

    /**
     * durationDistanceFactor: multiplies current durationMinutes/distanceMeters
     * (softening reduces volume, factor &lt; 1; progression never touches volume, factor = 1).
     * paceFactor: multiplies current targetPaceSeconds — since it's seconds-per-km, SLOWER
     * pace means a HIGHER number (softening factor &gt; 1); tightening/progression means
     * FASTER, a LOWER number (factor &lt; 1).
     */
    record Decision(String newWorkoutType, double durationDistanceFactor, double paceFactor, String reason) {
        static Decision downgradeAndSoften(String newType, double pct, String reason) {
            return new Decision(newType, 1 - pct, 1 + pct, reason);
        }
        static Decision softenOnly(double pct, String reason) {
            return new Decision(null, 1 - pct, 1 + pct, reason);
        }
        static Decision tightenPaceOnly(double pct, String reason) {
            return new Decision(null, 1.0, 1 - pct, reason);
        }
    }
}
