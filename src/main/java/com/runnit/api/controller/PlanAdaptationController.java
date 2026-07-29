package com.runnit.api.controller;

import com.runnit.api.dto.AdaptationResponse;
import com.runnit.api.model.Plan;
import com.runnit.api.model.PlanAdaptation;
import com.runnit.api.repository.PlanAdaptationRepository;
import com.runnit.api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanAdaptationController {

    private final PlanRepository planRepository;
    private final PlanAdaptationRepository planAdaptationRepository;

    /** GET /api/plans/active/adaptations — recent adaptation history for the active plan */
    @GetMapping("/active/adaptations")
    public ResponseEntity<?> getActiveAdaptations(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findByUserIdAndActiveTrue(userId).orElse(null);
            if (plan == null) {
                return ResponseEntity.status(404).body(Map.of("error", "No active plan found"));
            }
            List<AdaptationResponse> adaptations = planAdaptationRepository
                    .findByPlanIdOrderByCreatedAtDesc(plan.getId())
                    .stream().map(this::toResponse).collect(Collectors.toList());
            return ResponseEntity.ok(adaptations);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private AdaptationResponse toResponse(PlanAdaptation a) {
        AdaptationResponse r = new AdaptationResponse();
        r.setId(a.getId());
        r.setPlanWorkoutId(a.getPlanWorkoutId());
        r.setTriggeredBy(a.getTriggeredBy());
        r.setTriggerActivityId(a.getTriggerActivityId());
        r.setReason(a.getReason());
        r.setFieldChanged(a.getFieldChanged());
        r.setOldValue(a.getOldValue());
        r.setNewValue(a.getNewValue());
        r.setCreatedAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        return r;
    }
}
