package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.Activity.SportType;
import com.runnit.api.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AthleteArchetypeService {

    private final ActivityRepository activityRepository;

    // 90-day sliding window for classification
    private static final int WINDOW_DAYS = 90;
    // Minimum activities required to classify (otherwise default to GRINDER)
    private static final int MIN_ACTIVITIES = 5;

    public enum Archetype {
        THE_HYBRID_ATHLETE(
                "The Hybrid Athlete",
                "You crush multiple disciplines. Built different — triathlete energy."),
        THE_ENDURANCE_BEAST(
                "The Endurance Beast",
                "Long miles, big suffering. You go when everyone else quits."),
        THE_EXPLORER(
                "The Explorer",
                "Trails, elevation, new routes. Running is your way of seeing the world."),
        THE_COMPETITOR(
                "The Competitor",
                "Race results. PRs. Podiums. You're always chasing the clock."),
        THE_GRINDER(
                "The Grinder",
                "Consistent, relentless, disciplined. You show up every single day.");

        public final String label;
        public final String tagline;

        Archetype(String label, String tagline) {
            this.label = label;
            this.tagline = tagline;
        }
    }

    /**
     * Classify an athlete's archetype from their last 90 days of activities.
     * Classification is ordered by specificity — more specific traits win first.
     */
    public Archetype compute(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(WINDOW_DAYS);
        List<Activity> activities = activityRepository.findByUserIdSince(userId, since);

        if (activities.size() < MIN_ACTIVITIES) {
            return Archetype.THE_GRINDER; // Not enough data — default
        }

        int total = activities.size();

        // Sport type distribution
        Map<SportType, Long> bySport = activities.stream()
                .collect(Collectors.groupingBy(Activity::getSportType, Collectors.counting()));

        long bikeCount = bySport.getOrDefault(SportType.BIKE, 0L);
        long swimCount = bySport.getOrDefault(SportType.SWIM, 0L);
        long hikeCount = bySport.getOrDefault(SportType.HIKE, 0L);
        long runCount  = bySport.getOrDefault(SportType.RUN, 0L);

        // Ratio of cycling + swimming to total (multi-sport signal)
        double multiSportRatio = (double) (bikeCount + swimCount) / total;

        // Average run distance (meters) — only for RUN activities with distance
        double avgRunDistance = activities.stream()
                .filter(a -> a.getSportType() == SportType.RUN && a.getDistanceMeters() != null)
                .mapToInt(Activity::getDistanceMeters)
                .average()
                .orElse(0);

        // Average elevation gain per activity
        double avgElevation = activities.stream()
                .filter(a -> a.getElevationGain() != null)
                .mapToInt(Activity::getElevationGain)
                .average()
                .orElse(0);

        // Weekly frequency over the 90-day window (~12.86 weeks)
        double weeklyFreq = total / 12.86;

        // Average pace in min/km — lower = faster
        double avgPace = activities.stream()
                .filter(a -> a.getAveragePace() != null && a.getAveragePace() > 0)
                .mapToDouble(Activity::getAveragePace)
                .average()
                .orElse(0);

        // ── Classification (ordered by specificity) ────────────────────────

        // 1. Hybrid Athlete: 25%+ of activities are cycling or swimming
        if (multiSportRatio >= 0.25) {
            return Archetype.THE_HYBRID_ATHLETE;
        }

        // 2. Endurance Beast: avg run distance > 15 km
        if (runCount > 0 && avgRunDistance >= 15_000) {
            return Archetype.THE_ENDURANCE_BEAST;
        }

        // 3. Explorer: significant hiking OR high average elevation gain
        if (hikeCount >= 3 || avgElevation >= 200) {
            return Archetype.THE_EXPLORER;
        }

        // 4. Competitor: sub-5:30 min/km average pace (speed-focused runner)
        if (avgPace > 0 && avgPace < 5.5) {
            return Archetype.THE_COMPETITOR;
        }

        // 5. Grinder: 5+ activities per week
        if (weeklyFreq >= 5.0) {
            return Archetype.THE_GRINDER;
        }

        // Default
        return Archetype.THE_GRINDER;
    }
}
