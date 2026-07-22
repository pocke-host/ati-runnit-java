package com.runnit.api.controller;

import com.runnit.api.service.WhoopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/whoop")
@RequiredArgsConstructor
public class WhoopIntegrationController {

    private final WhoopService whoopService;

    /** GET /api/integrations/whoop/connect */
    @GetMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String authUrl = whoopService.buildAuthorizationUrl(userId);
            return ResponseEntity.ok(Map.of("url", authUrl));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/integrations/whoop/callback — public, WHOOP redirects here after consent */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String redirectUrl;
        if (error != null || code == null || state == null) {
            redirectUrl = whoopService.getFrontendUrl() + "/devices?error=whoop_denied";
        } else {
            try {
                redirectUrl = whoopService.handleCallback(code, state);
            } catch (Exception e) {
                log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
                redirectUrl = whoopService.getFrontendUrl() + "/devices?error=whoop_failed";
            }
        }
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }

    /** POST /api/integrations/whoop/mobile-callback */
    @PostMapping("/mobile-callback")
    public ResponseEntity<?> mobileCallback(@RequestBody Map<String, String> body) {
        String code  = body.get("code");
        String state = body.get("state");
        if (code == null || state == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "code and state are required"));
        }
        try {
            whoopService.handleCallback(code, state);
            return ResponseEntity.ok(Map.of("connected", true));
        } catch (Exception e) {
            log.error("Mobile WHOOP callback failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "WHOOP connection failed"));
        }
    }

    /** GET /api/integrations/whoop/status */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(whoopService.getStatus(userId));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/whoop/sync — manual sync */
    @PostMapping("/sync")
    public ResponseEntity<?> sync(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            int count = whoopService.syncActivities(userId);
            return ResponseEntity.ok(Map.of("imported", count, "message", count + " workouts synced"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/whoop/disconnect */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            whoopService.disconnect(userId);
            return ResponseEntity.ok(Map.of("message", "WHOOP disconnected"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
