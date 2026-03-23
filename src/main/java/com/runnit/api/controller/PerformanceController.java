package com.runnit.api.controller;

import com.runnit.api.dto.PerformanceResponse;
import com.runnit.api.service.PerformanceIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceIntelligenceService performanceService;

    /**
     * GET /api/users/me/performance
     *
     * Returns a full performance intelligence summary derived from the
     * authenticated user's activity history and personal records:
     *
     * - fitnessScore + disciplineScore (0–100)
     * - disciplineLevel (BEGINNER / CONSISTENT / DISCIPLINED / ELITE)
     * - trainingConsistency (% of last 8 weeks with ≥ 1 run)
     * - weeklyVolumeTrend (last 8 weeks, oldest → newest)
     * - vo2maxEstimate (mL/kg/min via Jack Daniels VDOT — null if no PR data)
     * - predictedRaceTimes (5K, 10K, half, marathon via Riegel's formula)
     * - trainingGaps (actionable coaching-style insight strings)
     * - currentStreakDays (consecutive days with ≥ 1 activity)
     */
    @GetMapping("/performance")
    public ResponseEntity<?> getPerformance(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            PerformanceResponse response = performanceService.compute(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to compute performance intelligence: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
