package com.runnit.api.controller;

import com.runnit.api.dto.TrainingPlanRequest;
import com.runnit.api.service.TrainingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService planService;

    // ─── Plan CRUD ────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createPlan(@Valid @RequestBody TrainingPlanRequest request, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.createPlan(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getPublishedPlans(
            @RequestParam(required = false) String sportType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.getPublishedPlans(userId, sportType, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.getMyPlans(userId, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{planId}")
    public ResponseEntity<?> getPlanById(@PathVariable Long planId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.getPlanById(planId, userId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{planId}")
    public ResponseEntity<?> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody TrainingPlanRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.updatePlan(planId, userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deletePlan(@PathVariable Long planId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            planService.deletePlan(planId, userId);
            return ResponseEntity.ok(Map.of("message", "Plan deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Subscriptions ────────────────────────────────────────────────────────

    @PostMapping("/{planId}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable Long planId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.subscribe(userId, planId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{planId}/subscribe")
    public ResponseEntity<?> unsubscribe(@PathVariable Long planId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            planService.unsubscribe(userId, planId);
            return ResponseEntity.ok(Map.of("message", "Unsubscribed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<?> getMySubscriptions(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.getMySubscriptions(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<?> getSubscription(@PathVariable Long subscriptionId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.getSubscription(subscriptionId, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/subscriptions/{subscriptionId}/advance-week")
    public ResponseEntity<?> advanceWeek(@PathVariable Long subscriptionId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.advanceWeek(subscriptionId, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/subscriptions/{subscriptionId}/adapt")
    public ResponseEntity<?> adaptWeek(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "0") int volumeAdjustmentPercent,
            @RequestParam(required = false) String intensityAdjustment,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(planService.adaptWeek(subscriptionId, userId, volumeAdjustmentPercent, intensityAdjustment, notes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
