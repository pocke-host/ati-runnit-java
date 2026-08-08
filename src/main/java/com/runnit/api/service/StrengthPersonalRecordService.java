package com.runnit.api.service;

import com.runnit.api.dto.StrengthHistoryPoint;
import com.runnit.api.dto.StrengthPRResponse;
import com.runnit.api.dto.StrengthVolumeResponse;
import com.runnit.api.repository.StrengthSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes strength personal records and volume totals on-read from logged sets.
 * Nothing here is materialized — every call re-scans the requesting user's set rows, which is
 * fine at expected per-user set volumes but would need a materialized rollup if that stops holding.
 */
@Service
@RequiredArgsConstructor
public class StrengthPersonalRecordService {

    private final StrengthSetRepository strengthSetRepository;

    public StrengthPRResponse computePRs(Long userId, String exerciseName) {
        StrengthPRResponse response = new StrengthPRResponse(exerciseName);
        List<Object[]> rows = strengthSetRepository.findSetRowsByUserAndExerciseName(userId, exerciseName);

        StrengthPRResponse.PRDetail heaviestWeight = null;
        StrengthPRResponse.PRDetail estimatedOneRepMax = null;
        Map<Long, Double> volumeByActivity = new HashMap<>();
        Map<Long, LocalDateTime> performedAtByActivity = new HashMap<>();

        for (Object[] row : rows) {
            Long activityId = (Long) row[0];
            LocalDateTime performedAt = (LocalDateTime) row[1];
            Integer reps = (Integer) row[2];
            Double weightKg = (Double) row[3];
            Boolean isWarmup = (Boolean) row[4];

            performedAtByActivity.put(activityId, performedAt);
            if (Boolean.TRUE.equals(isWarmup) || weightKg == null || reps == null) continue;

            if (heaviestWeight == null || weightKg > heaviestWeight.getValue()) {
                heaviestWeight = new StrengthPRResponse.PRDetail(weightKg, reps, activityId, performedAt);
            }

            // Epley formula: estimated 1RM = weight * (1 + reps / 30)
            double estimated1RM = weightKg * (1 + reps / 30.0);
            if (estimatedOneRepMax == null || estimated1RM > estimatedOneRepMax.getValue()) {
                estimatedOneRepMax = new StrengthPRResponse.PRDetail(estimated1RM, reps, activityId, performedAt);
            }

            volumeByActivity.merge(activityId, weightKg * reps, Double::sum);
        }

        StrengthPRResponse.PRDetail bestRepsAtHeaviestWeight = null;
        if (heaviestWeight != null) {
            for (Object[] row : rows) {
                Boolean isWarmup = (Boolean) row[4];
                Double weightKg = (Double) row[3];
                Integer reps = (Integer) row[2];
                if (Boolean.TRUE.equals(isWarmup) || weightKg == null || reps == null) continue;
                if (!weightKg.equals(heaviestWeight.getValue())) continue;

                Long activityId = (Long) row[0];
                LocalDateTime performedAt = (LocalDateTime) row[1];
                if (bestRepsAtHeaviestWeight == null || reps > bestRepsAtHeaviestWeight.getReps()) {
                    bestRepsAtHeaviestWeight = new StrengthPRResponse.PRDetail(weightKg, reps, activityId, performedAt);
                }
            }
        }

        StrengthPRResponse.PRDetail mostVolumeInSession = volumeByActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> new StrengthPRResponse.PRDetail(e.getValue(), null, e.getKey(), performedAtByActivity.get(e.getKey())))
                .orElse(null);

        response.setHeaviestWeight(heaviestWeight);
        response.setBestRepsAtHeaviestWeight(bestRepsAtHeaviestWeight);
        response.setEstimatedOneRepMax(estimatedOneRepMax);
        response.setMostVolumeInSession(mostVolumeInSession);
        return response;
    }

    public StrengthVolumeResponse computeVolume(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = strengthSetRepository.findSetRowsByUserSince(userId, since);

        double totalVolumeKg = 0;
        int totalSets = 0;
        Set<Long> sessionIds = new HashSet<>();

        for (Object[] row : rows) {
            Long activityId = (Long) row[0];
            Integer reps = (Integer) row[2];
            Double weightKg = (Double) row[3];
            Boolean isWarmup = (Boolean) row[4];

            sessionIds.add(activityId);
            totalSets++;
            if (!Boolean.TRUE.equals(isWarmup) && weightKg != null && reps != null) {
                totalVolumeKg += weightKg * reps;
            }
        }

        return new StrengthVolumeResponse(days, totalVolumeKg, totalSets, sessionIds.size());
    }

    /** One point per session (not per set) for a given exercise, oldest-first — the time series a progress chart plots. */
    public List<StrengthHistoryPoint> computeHistory(Long userId, String exerciseName) {
        List<Object[]> rows = strengthSetRepository.findSetRowsByUserAndExerciseName(userId, exerciseName);

        Map<Long, LocalDateTime> performedAtByActivity = new HashMap<>();
        Map<Long, Double> topWeightByActivity = new HashMap<>();
        Map<Long, Double> volumeByActivity = new HashMap<>();
        Map<Long, Integer> repsByActivity = new HashMap<>();
        Map<Long, Integer> setCountByActivity = new HashMap<>();
        Map<Long, Double> oneRepMaxByActivity = new HashMap<>();

        for (Object[] row : rows) {
            Long activityId = (Long) row[0];
            LocalDateTime performedAt = (LocalDateTime) row[1];
            Integer reps = (Integer) row[2];
            Double weightKg = (Double) row[3];
            Boolean isWarmup = (Boolean) row[4];

            performedAtByActivity.put(activityId, performedAt);
            setCountByActivity.merge(activityId, 1, Integer::sum);

            if (Boolean.TRUE.equals(isWarmup) || weightKg == null || reps == null) continue;

            topWeightByActivity.merge(activityId, weightKg, Math::max);
            volumeByActivity.merge(activityId, weightKg * reps, Double::sum);
            repsByActivity.merge(activityId, reps, Integer::sum);

            double estimated1RM = weightKg * (1 + reps / 30.0);
            oneRepMaxByActivity.merge(activityId, estimated1RM, Math::max);
        }

        List<StrengthHistoryPoint> history = new ArrayList<>();
        for (Long activityId : performedAtByActivity.keySet()) {
            history.add(new StrengthHistoryPoint(
                    activityId,
                    performedAtByActivity.get(activityId),
                    topWeightByActivity.get(activityId),
                    volumeByActivity.get(activityId),
                    repsByActivity.get(activityId),
                    setCountByActivity.get(activityId),
                    oneRepMaxByActivity.get(activityId)
            ));
        }
        history.sort(Comparator.comparing(StrengthHistoryPoint::getPerformedAt));
        return history;
    }
}
