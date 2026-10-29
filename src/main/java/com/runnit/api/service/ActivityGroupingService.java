package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Links provider rows that represent one workout without deleting either row.
 *
 * The first iteration intentionally only groups Garmin and WHOOP. Those are the
 * providers for which RUNNIT has a complete activity path and a clear product
 * rule: Garmin is the route/performance source and WHOOP is the recovery/load
 * context. Other integrations remain available for comparison, but are not
 * silently merged until their timing semantics are verified.
 */
@Service
@RequiredArgsConstructor
public class ActivityGroupingService {

    private static final Set<Activity.Source> GROUPABLE_SOURCES =
            EnumSet.of(Activity.Source.GARMIN, Activity.Source.WHOOP);
    private static final long MATCH_WINDOW_SECONDS = 30 * 60L;

    private final ActivityRepository activityRepository;

    /**
     * Link a newly saved provider activity to the best matching activity, if
     * one exists. The returned value is the canonical row that should be used
     * by callers for user-facing links and follow-up work.
     */
    @Transactional
    public Activity groupIfMatching(Activity activity) {
        if (!isGroupable(activity) || activity.getId() == null) return activity;

        Activity match = findBestMatch(activity);
        if (match == null) return activity;

        Activity canonical = preferredCanonical(activity, match);
        Activity secondary = canonical.getId().equals(activity.getId()) ? match : activity;

        // If the older provider row was already linked, do not overwrite its
        // established grouping with a less authoritative child row.
        if (secondary.getCanonicalActivityId() == null
                || !secondary.getCanonicalActivityId().equals(canonical.getId())) {
            secondary.setCanonicalActivityId(canonical.getId());
            secondary.setSourceMatchConfidence(matchConfidence(activity, match));
            activityRepository.save(secondary);
        }
        return canonical;
    }

    /** Return the best Garmin/WHOOP match for detail-page comparisons. */
    @Transactional(readOnly = true)
    public List<Activity> findMatchingSources(Activity activity) {
        if (!isGroupable(activity)) return List.of();
        Activity match = findBestMatch(activity);
        return match == null ? List.of() : List.of(match);
    }

    private Activity findBestMatch(Activity activity) {
        LocalDateTime occurredAt = occurredAt(activity);
        if (occurredAt == null) return null;

        return activityRepository.findByUserIdNearTime(
                        activity.getUser().getId(),
                        occurredAt.minusSeconds(MATCH_WINDOW_SECONDS),
                        occurredAt.plusSeconds(MATCH_WINDOW_SECONDS))
                .stream()
                .filter(candidate -> candidate.getId() != null
                        && !candidate.getId().equals(activity.getId()))
                .filter(candidate -> candidate.getCanonicalActivityId() == null)
                .filter(candidate -> isGroupable(candidate)
                        && candidate.getSource() != activity.getSource())
                .filter(candidate -> sameSport(activity, candidate))
                .filter(candidate -> representsSameWorkout(activity, candidate, occurredAt))
                .min(Comparator.comparingDouble(candidate -> matchScore(activity, candidate, occurredAt)))
                .orElse(null);
    }

    private boolean isGroupable(Activity activity) {
        return activity != null
                && activity.getUser() != null
                && activity.getSource() != null
                && GROUPABLE_SOURCES.contains(activity.getSource());
    }

    private Activity preferredCanonical(Activity left, Activity right) {
        if (left.getSource() == Activity.Source.GARMIN) return left;
        if (right.getSource() == Activity.Source.GARMIN) return right;
        return left.getId() < right.getId() ? left : right;
    }

    private boolean sameSport(Activity left, Activity right) {
        return left.getSportType() == right.getSportType()
                || left.getSportType() == Activity.SportType.OTHER
                || right.getSportType() == Activity.SportType.OTHER;
    }

    private boolean representsSameWorkout(Activity activity, Activity candidate, LocalDateTime occurredAt) {
        long secondsApart = timeDistanceSeconds(candidate, occurredAt);
        if (secondsApart > MATCH_WINDOW_SECONDS) return false;

        int duration = value(activity.getDurationSeconds());
        int candidateDuration = value(candidate.getDurationSeconds());
        if (duration > 0 && candidateDuration > 0) {
            int tolerance = Math.max(180, Math.round(Math.min(duration, candidateDuration) * 0.20f));
            if (Math.abs(duration - candidateDuration) > tolerance) return false;
        }

        int distance = value(activity.getDistanceMeters());
        int candidateDistance = value(candidate.getDistanceMeters());
        if (distance > 0 && candidateDistance > 0) {
            int tolerance = Math.max(750, Math.round(Math.min(distance, candidateDistance) * 0.15f));
            if (Math.abs(distance - candidateDistance) > tolerance) return false;
        }

        // A pair with no duration and no distance is not safe to merge. Two
        // nearby zero-metric records could easily be separate walks.
        return duration > 0 || candidateDuration > 0 || distance > 0 || candidateDistance > 0;
    }

    private double matchScore(Activity activity, Activity candidate, LocalDateTime occurredAt) {
        double score = timeDistanceSeconds(candidate, occurredAt) / (double) MATCH_WINDOW_SECONDS;
        int duration = value(activity.getDurationSeconds());
        int candidateDuration = value(candidate.getDurationSeconds());
        if (duration > 0 && candidateDuration > 0) {
            score += Math.abs(duration - candidateDuration) / (double) Math.max(duration, candidateDuration);
        }
        int distance = value(activity.getDistanceMeters());
        int candidateDistance = value(candidate.getDistanceMeters());
        if (distance > 0 && candidateDistance > 0) {
            score += Math.abs(distance - candidateDistance) / (double) Math.max(distance, candidateDistance);
        }
        return score;
    }

    private double matchConfidence(Activity activity, Activity candidate) {
        LocalDateTime occurredAt = occurredAt(activity);
        if (occurredAt == null) return 0d;
        return Math.max(0d, Math.min(1d, 1d - (matchScore(activity, candidate, occurredAt) / 2d)));
    }

    private LocalDateTime occurredAt(Activity activity) {
        return activity.getPerformedAt() != null ? activity.getPerformedAt() : activity.getCreatedAt();
    }

    private long timeDistanceSeconds(Activity activity, LocalDateTime occurredAt) {
        LocalDateTime candidateAt = occurredAt(activity);
        return candidateAt == null ? Long.MAX_VALUE : Math.abs(Duration.between(occurredAt, candidateAt).getSeconds());
    }

    private int value(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
