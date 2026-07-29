package com.runnit.api.controller;

import com.runnit.api.service.TrainingLoadService;
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
@RequestMapping("/api/training-load")
@RequiredArgsConstructor
public class TrainingLoadController {

    private final TrainingLoadService trainingLoadService;

    /** GET /api/training-load — current user's fitness/fatigue (CTL/ATL/TSB/ACWR) */
    @GetMapping
    public ResponseEntity<?> getMyTrainingLoad(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(trainingLoadService.computeMetrics(userId));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
