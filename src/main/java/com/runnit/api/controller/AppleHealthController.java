package com.runnit.api.controller;

import com.runnit.api.service.AppleHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/apple-health")
@RequiredArgsConstructor
public class AppleHealthController {

    private final AppleHealthService appleHealthService;

    /** GET /api/integrations/apple-health/status */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(appleHealthService.getStatus(userId));
        } catch (Exception e) {
            log.error("Apple Health status failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/apple-health/connect — called once native HealthKit permission is granted */
    @PostMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            appleHealthService.connect(userId);
            return ResponseEntity.ok(Map.of("connected", true));
        } catch (Exception e) {
            log.error("Apple Health connect failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/apple-health/sync — body: { "samples": [ { externalId, sportType, durationSeconds, distanceMeters, calories, performedAt }, ... ] } */
    @PostMapping("/sync")
    public ResponseEntity<?> sync(Authentication auth, @RequestBody Map<String, Object> body) {
        try {
            Long userId = (Long) auth.getPrincipal();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> samples = (List<Map<String, Object>>) body.get("samples");
            if (samples == null) samples = List.of();

            int imported = appleHealthService.syncActivities(userId, samples);
            return ResponseEntity.ok(Map.of("imported", imported, "message", imported + " activities synced"));
        } catch (Exception e) {
            log.error("Apple Health sync failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/apple-health/disconnect */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            appleHealthService.disconnect(userId);
            return ResponseEntity.ok(Map.of("message", "Apple Health disconnected"));
        } catch (Exception e) {
            log.error("Apple Health disconnect failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
