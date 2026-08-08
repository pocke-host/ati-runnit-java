package com.runnit.api.controller;

import com.runnit.api.dto.StrengthHistoryPoint;
import com.runnit.api.dto.StrengthPRResponse;
import com.runnit.api.dto.StrengthVolumeResponse;
import com.runnit.api.repository.StrengthExerciseRepository;
import com.runnit.api.service.StrengthPersonalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/strength")
@RequiredArgsConstructor
public class StrengthController {

    private final StrengthExerciseRepository strengthExerciseRepository;
    private final StrengthPersonalRecordService personalRecordService;

    @GetMapping("/exercises")
    public ResponseEntity<?> getExerciseNames(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<String> names = strengthExerciseRepository.findDistinctExerciseNamesByUserId(userId);
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/prs")
    public ResponseEntity<?> getPRs(@RequestParam String exercise, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            StrengthPRResponse prs = personalRecordService.computePRs(userId, exercise);
            return ResponseEntity.ok(prs);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/volume")
    public ResponseEntity<?> getVolume(@RequestParam(defaultValue = "7") int days, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            StrengthVolumeResponse volume = personalRecordService.computeVolume(userId, days);
            return ResponseEntity.ok(volume);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam String exercise, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<StrengthHistoryPoint> history = personalRecordService.computeHistory(userId, exercise);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
