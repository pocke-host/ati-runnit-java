package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.Moment;
import com.runnit.api.repository.MomentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Turns a freshly-synced device activity into a Moment, so it gets the richer
 * feed treatment instead of only ever being a plain activity card. Respects
 * the existing one-Moment-per-user-per-day design (moments.unique_user_day) —
 * if the user already has a Moment today (manual or a prior auto-created
 * one), this is a no-op rather than a conflict. Manual activities are never
 * touched: the user already has a dedicated "Create Moment" flow if they
 * want one, so auto-creating from a manual entry would just be redundant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoMomentService {

    private final MomentRepository momentRepository;

    /** Same reasoning as AdaptivePlanService's own guard: a bulk historical
     * backfill (up to hundreds of activities on initial device connect)
     * would otherwise try to create a Moment for every distinct past day —
     * a flood, not a highlight. Only react to activities that just happened. */
    private static final int BACKFILL_CUTOFF_HOURS = 48;

    @Transactional
    public void onActivityRecorded(Activity activity) {
        if (activity == null || activity.getUser() == null) return;
        if (activity.getSource() == Activity.Source.MANUAL) return;

        LocalDateTime performedAt = activity.getPerformedAt() != null ? activity.getPerformedAt() : activity.getCreatedAt();
        if (performedAt == null || performedAt.isBefore(LocalDateTime.now().minusHours(BACKFILL_CUTOFF_HOURS))) return;

        LocalDate dayKey = LocalDate.now(ZoneId.of("UTC"));
        if (momentRepository.findByUserAndDayKey(activity.getUser(), dayKey).isPresent()) return;

        Moment moment = Moment.builder()
                .user(activity.getUser())
                .activity(activity)
                .caption(buildCaption(activity))
                .dayKey(dayKey)
                .build();
        momentRepository.save(moment);
    }

    private String buildCaption(Activity activity) {
        boolean imperial = !"metric".equalsIgnoreCase(activity.getUser().getUnitSystem());
        StringBuilder caption = new StringBuilder(displaySport(activity.getSportType()));

        Integer distanceMeters = activity.getDistanceMeters();
        if (distanceMeters != null && distanceMeters > 0) {
            caption.append(" • ").append(formatDistance(distanceMeters, imperial));
        }
        Integer durationSeconds = activity.getDurationSeconds();
        if (durationSeconds != null && durationSeconds > 0) {
            caption.append(" • ").append(formatDuration(durationSeconds));
        }
        return caption.toString();
    }

    private String displaySport(Activity.SportType sportType) {
        if (sportType == null) return "Workout";
        return switch (sportType) {
            case RUN -> "Run";
            case BIKE -> "Ride";
            case SWIM -> "Swim";
            case HIKE -> "Hike";
            case WALK -> "Walk";
            case OTHER -> "Workout";
        };
    }

    private String formatDistance(int meters, boolean imperial) {
        if (imperial) {
            double miles = meters / 1609.34;
            return String.format("%.1f mi", miles);
        }
        double km = meters / 1000.0;
        return String.format("%.1f km", km);
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) return String.format("%dh %02dm", hours, minutes);
        return String.format("%d min", minutes);
    }
}
