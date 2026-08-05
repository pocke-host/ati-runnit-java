package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Training load intelligence — chronic/acute training load (CTL/ATL), training
 * stress balance (TSB), and acute:chronic workload ratio (ACWR), computed from
 * activity history. Same model TrainingPeaks' PMC and most endurance coaching
 * tools use (Banister-style exponentially-weighted moving averages), adapted
 * to work without power-meter/HR-zone data: load is duration-weighted by sport
 * rather than TRIMP, since duration is the one input every activity actually has.
 */
@Service
@RequiredArgsConstructor
public class TrainingLoadService {

    private final ActivityRepository activityRepository;

    // Rough RPE-equivalent weighting per minute, running = 1.0 baseline. STRENGTH is a
    // known simplification: this model weights by clock duration, but a heavy lifting
    // session's actual load doesn't track duration the way continuous cardio does (a
    // typical hour includes significant rest between sets at low load, diluting the
    // high-effort working sets) — 0.7 reflects average effort across total session time
    // including rest, not peak per-rep RPE. The more accurate fix (compute load from
    // strength_sets' reps*weight instead of duration) needs this service to join into
    // strength data it doesn't touch today — out of scope here, flagged for later.
    private static final Map<Activity.SportType, Double> SPORT_INTENSITY = Map.of(
            Activity.SportType.RUN, 1.0,
            Activity.SportType.BIKE, 0.75,
            Activity.SportType.SWIM, 1.1,
            Activity.SportType.HIKE, 0.6,
            Activity.SportType.STRENGTH, 0.7,
            Activity.SportType.WALK, 0.3,
            Activity.SportType.OTHER, 0.8
    );

    private static final int WINDOW_DAYS = 90;
    private static final double CTL_DAYS = 42.0;
    private static final double ATL_DAYS = 7.0;
    private static final int MIN_ACTIVITIES_FOR_SIGNAL = 3;

    public Map<String, Object> computeMetrics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(WINDOW_DAYS).atStartOfDay();
        List<Activity> activities = activityRepository.findByUserIdSince(userId, since);

        double[] dailyLoad = new double[WINDOW_DAYS];
        for (Activity a : activities) {
            if (a.getCreatedAt() == null || a.getDurationSeconds() == null) continue;
            LocalDate day = a.getCreatedAt().toLocalDate();
            int daysAgo = (int) ChronoUnit.DAYS.between(day, today);
            if (daysAgo < 0 || daysAgo >= WINDOW_DAYS) continue;
            double minutes = a.getDurationSeconds() / 60.0;
            double intensity = SPORT_INTENSITY.getOrDefault(a.getSportType(), 0.8);
            dailyLoad[WINDOW_DAYS - 1 - daysAgo] += minutes * intensity;
        }

        double ctl = 0, atl = 0, ctlWeekAgo = 0;
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < WINDOW_DAYS; i++) {
            ctl = ctl * (1 - 1 / CTL_DAYS) + dailyLoad[i] * (1 / CTL_DAYS);
            atl = atl * (1 - 1 / ATL_DAYS) + dailyLoad[i] * (1 / ATL_DAYS);
            if (i == WINDOW_DAYS - 1 - 7) ctlWeekAgo = ctl;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", today.minusDays(WINDOW_DAYS - 1 - i).toString());
            point.put("load", round1(dailyLoad[i]));
            point.put("ctl", round1(ctl));
            point.put("atl", round1(atl));
            series.add(point);
        }

        double tsb = ctl - atl;
        double acwr = ctl > 0.5 ? atl / ctl : 0;
        boolean hasEnoughData = activities.size() >= MIN_ACTIVITIES_FOR_SIGNAL;

        String riskLabel;
        if (!hasEnoughData) riskLabel = "BUILDING_BASELINE";
        else if (acwr > 1.5) riskLabel = "HIGH_RISK";
        else if (acwr < 0.8) riskLabel = "DETRAINING";
        else riskLabel = "OPTIMAL";

        String fitnessTrend;
        if (ctl > ctlWeekAgo + 0.5) fitnessTrend = "RISING";
        else if (ctl < ctlWeekAgo - 0.5) fitnessTrend = "FALLING";
        else fitnessTrend = "STABLE";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ctl", round1(ctl));
        result.put("atl", round1(atl));
        result.put("tsb", round1(tsb));
        result.put("acwr", Math.round(acwr * 100) / 100.0);
        result.put("riskLabel", riskLabel);
        result.put("fitnessTrend", fitnessTrend);
        result.put("hasEnoughData", hasEnoughData);
        result.put("activityCount90d", activities.size());
        result.put("series", series.subList(WINDOW_DAYS - 42, WINDOW_DAYS)); // last 42 days for charting
        return result;
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
