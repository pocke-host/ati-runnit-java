package com.runnit.api.util;

import com.runnit.api.model.Plan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Derives calendar weeks for a Plan's workouts. PlanWorkout.day is an ordinal
 * slot within the week (1st/2nd/3rd workout), not a literal weekday — the
 * plan-creation wizards (PlanController, TrainingPlans.vue) assign it as a
 * simple loop index, so no per-day calendar date can be derived reliably.
 * Only the week boundary (anchored to Plan.startDate) is meaningful.
 */
public final class PlanDateUtil {

    private PlanDateUtil() {}

    /** Monday-anchored start date of the given 1-based week number. Null if the plan has no startDate. */
    public static LocalDate weekStartDate(Plan plan, Integer weekNumber) {
        if (plan.getStartDate() == null || weekNumber == null) return null;
        return plan.getStartDate().plusWeeks(weekNumber - 1L);
    }

    /** 1-based week number (matching TrainingBlockService.resolveCurrentWeek's convention) containing the given date. Null if the plan has no startDate. */
    public static Integer weekNumberContaining(Plan plan, LocalDate date) {
        if (plan.getStartDate() == null || date == null) return null;
        long weeksSinceStart = ChronoUnit.WEEKS.between(plan.getStartDate(), date);
        return (int) weeksSinceStart + 1;
    }
}
