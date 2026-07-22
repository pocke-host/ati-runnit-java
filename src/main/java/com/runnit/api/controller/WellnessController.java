package com.runnit.api.controller;

import com.runnit.api.model.WellnessDaily;
import com.runnit.api.repository.WellnessDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessDailyRepository wellnessDailyRepository;

    /** GET /api/wellness/today — most recent day's recovery/sleep/strain snapshot */
    @GetMapping("/today")
    public ResponseEntity<?> getToday(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            LocalDate today = LocalDate.now();
            // Fall back a few days in case today's cycle hasn't scored yet (e.g. still asleep)
            for (int i = 0; i < 3; i++) {
                LocalDate date = today.minusDays(i);
                var row = wellnessDailyRepository.findByUserIdAndDate(userId, date);
                if (row.isPresent()) return ResponseEntity.ok(toMap(row.get()));
            }
            return ResponseEntity.ok(Map.of("available", false));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/wellness?start=YYYY-MM-DD&end=YYYY-MM-DD — history for charting */
    @GetMapping
    public ResponseEntity<?> getRange(
            @RequestParam String start,
            @RequestParam String end,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<Map<String, Object>> days = wellnessDailyRepository
                    .findByUserIdAndDateBetweenOrderByDateDesc(userId, LocalDate.parse(start), LocalDate.parse(end))
                    .stream().map(this::toMap).toList();
            return ResponseEntity.ok(days);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(WellnessDaily w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", w.getDate().toString());
        m.put("source", w.getSource());
        m.put("recoveryScore", w.getRecoveryScore());
        m.put("hrvMilli", w.getHrvMilli());
        m.put("restingHeartRate", w.getRestingHeartRate());
        m.put("sleepPerformancePct", w.getSleepPerformancePct());
        m.put("sleepEfficiencyPct", w.getSleepEfficiencyPct());
        m.put("totalSleepMinutes", w.getTotalSleepMinutes());
        m.put("strain", w.getStrain());
        return m;
    }
}
