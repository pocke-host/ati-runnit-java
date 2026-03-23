package com.runnit.api.controller;

import com.runnit.api.service.GarminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/garmin")
@RequiredArgsConstructor
public class GarminIntegrationController {

    private final GarminService garminService;

    /** GET /api/integrations/garmin/connect — returns Garmin OAuth URL */
    @GetMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String authUrl = garminService.buildAuthorizationUrl(userId);
            return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/integrations/garmin/status */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(garminService.getStatus(userId));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/garmin/sync — manual sync */
    @PostMapping("/sync")
    public ResponseEntity<?> sync(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            int count = garminService.syncActivities(userId);
            return ResponseEntity.ok(Map.of("imported", count, "message", count + " activities synced"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/garmin/disconnect */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            garminService.disconnect(userId);
            return ResponseEntity.ok(Map.of("message", "Garmin disconnected"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
