// ========== ActivityController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.model.Activity;
import com.runnit.api.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    
    private final ActivityService activityService;
    
    @PostMapping
    public ResponseEntity<?> createActivity(
            @Valid @RequestBody ActivityRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityService.createActivity(userId, request);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Page<Activity> activities = activityService.getUserActivities(userId, page, size);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getActivity(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
